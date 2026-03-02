package com.bandhanbook.app.service;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.Image;
import com.bandhanbook.app.model.UploadContext;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.response.EventParticipantsResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.MatrimonyRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
import com.bandhanbook.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private final MatrimonyRepository matrimonyRepository;
    private final AgentRepository agentRepository;
    private final OrganizationRepository orgRepository;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final MessageUtil messageUtil;

    @Value("${images.base.path}")
    private String basePath;
    @Value("${images.candidate.path}")
    private String candidateImagePath;
    @Value("${images.agent.path}")
    private String agentImagePath;
    @Value("${images.organization.path}")
    private String organizationImagePath;
    @Value("${images.superUser.path}")
    private String superUserImagePath;


    @Autowired
    private ImageUploadService ImageUploadService;

    public Mono<String> uploadProfileImage(Users authUser, FilePart file) {

        validateImage(file);

        return userRepository.findById(authUser.getId())
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(user ->
                        resolveUploadContext(authUser)
                                .flatMap(ctx ->
                                        deleteExistingImage(user.getProfileImage())
                                                .then(ImageUploadService.upload(
                                                        file,
                                                        ctx.entityId(),
                                                        ctx.folder()
                                                ))
                                                .flatMap(image -> {
                                                    user.setProfileImage(image);
                                                    return userRepository.save(user);
                                                })
                                                .map(saved ->
                                                        ImageUploadService.getFullImageUrl(saved.getProfileImage())
                                                )
                                )
                );
    }

    private Mono<UploadContext> resolveUploadContext(Users authUser) {

        if (authUser.isCandidate()) {
            return authService.getMatrimonyDetails(authUser.getActiveRole().name(), authUser)
                    .flatMap(res -> {
                        String orgId = res.getEventParticipants()
                                .stream()
                                .findFirst()
                                .map(EventParticipantsResponse::getOrganizationId)
                                .orElseThrow(() -> new RecordNotFoundException(messageUtil.get("record.not.found")));

                        return matrimonyRepository.findByUserId(authUser.getId())
                                .map(candidate ->
                                        new UploadContext(
                                                candidate.getId().toHexString(),
                                                basePath + orgId + candidateImagePath + authUser.getId()
                                        )
                                );
                    });
        }

        if (authUser.isAgent()) {
            return agentRepository.findByUserId(authUser.getId())
                    .map(agent ->
                            new UploadContext(
                                    agent.getId().toHexString(),
                                    basePath + agent.getOrganizationId() + agentImagePath + authUser.getId()
                            )
                    );
        }

        if (authUser.isOrganization()) {
            return orgRepository.findByUserId(authUser.getId())
                    .map(org ->
                            new UploadContext(
                                    org.getId().toHexString(),
                                    basePath + org.getId() + organizationImagePath
                            )
                    );
        }
        if (authUser.isSuperUser()) {
            return Mono.just(
                    new UploadContext(
                            authUser.getId().toHexString(),
                            basePath + superUserImagePath + authUser.getId()
                    )
            );
        }

        return Mono.error(new RecordNotFoundException(messageUtil.get("unsupported.role.for.profile")));
    }

    public Mono<Void> removeProfileImage(Users authUser) {
        return userRepository.findById(authUser.getId()).flatMap(user -> {
            Image profile = user.getProfileImage();
            return deleteExistingImage(profile)
                    .then(Mono.fromCallable(() -> {
                        user.setProfileImage(null);
                        return user;
                    }))
                    .flatMap(userRepository::save)
                    .then();
        }).onErrorResume(e -> {
            log.error("Error removing profile image for user {}: {}", authUser.getId(), e.getMessage());
            return Mono.error(new RecordNotFoundException(messageUtil.get("profile.remove.error")));
        });
    }

    @Transactional
    public Flux<Image> uploadMatrimonyImages(Flux<FilePart> files, Users authUser) {

        return authService.getMatrimonyDetails(RoleNames.Candidate.name(), authUser)
                .flatMapMany(res -> {

                    String orgId = res.getEventParticipants().stream().findFirst().map(EventParticipantsResponse::getOrganizationId)
                            .orElseThrow(() -> new RecordNotFoundException(messageUtil.get("record.not.found")));

                    return matrimonyRepository.findByUserId(authUser.getId())
                            .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("candidate.profile.not.found"))))
                            .flatMapMany(candidate -> {
                                String folder = basePath + orgId + candidateImagePath + authUser.getId() + "/gallery";

                                return files.flatMap(file ->
                                                ImageUploadService.upload(file, candidate.getId().toHexString(), folder)
                                        ).collectList()
                                        .flatMapMany(images -> {
                                            if (candidate.getImages() == null) {
                                                candidate.setImages(images);
                                            } else {
                                                candidate.getImages().addAll(images);
                                            }
                                            return matrimonyRepository.save(candidate)
                                                    .thenMany(Flux.fromIterable(images));
                                        });
                            });
                });
    }


    public Mono<String> removeMatrimonyImages(String imageId, Users authUser) {
        return matrimonyRepository.findByUserId(authUser.getId())
                .switchIfEmpty(Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found"))))
                .flatMap(candidate -> {

                    if (candidate.getImages() == null || candidate.getImages().isEmpty()) {
                        return Mono.error(new RecordNotFoundException(messageUtil.get("record.not.found")));
                    }

                    Image image = candidate.getImages()
                            .stream()
                            .filter(img -> img.getId().equals(imageId))
                            .findFirst()
                            .orElseThrow(() -> new RecordNotFoundException(messageUtil.get("record.not.found")));

                    return deleteExistingImage(image)
                            .then(Mono.fromRunnable(() -> candidate.getImages().remove(image)))
                            .then(matrimonyRepository.save(candidate))
                            .thenReturn(messageUtil.get("images.removed"));
                });
    }


    public Mono<Void> deleteExistingImage(Image profile) {
        if (profile != null &&
                profile.getId() != null) {
            return ImageUploadService.delete(profile.getId()).onErrorResume(e -> Mono.empty());
        }
        return Mono.empty();
    }

    private void validateImage(FilePart file) {
        if (file == null ||
                file.headers().getContentType() == null ||
                !Objects.requireNonNull(file.headers().getContentType()).toString().startsWith("image/")) {
            throw new RecordNotFoundException(messageUtil.get("invalid.file.type"));
        }
    }
}
