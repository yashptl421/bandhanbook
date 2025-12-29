package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.RecordNotFoundException;
import com.bandhanbook.app.model.Image;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.response.EventParticipantsResponse;
import com.bandhanbook.app.repository.AgentRepository;
import com.bandhanbook.app.repository.MatrimonyRepository;
import com.bandhanbook.app.repository.OrganizationRepository;
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
import java.util.Optional;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.INVALID_FILE_TYPE;
import static com.bandhanbook.app.utilities.ErrorResponseMessages.RECORD_NOT_FOUND;
import static com.bandhanbook.app.utilities.SuccessResponseMessages.IMAGES_REMOVED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private final MatrimonyRepository matrimonyRepository;
    private final AgentRepository agentRepository;
    private final OrganizationRepository orgRepository;
    private final AuthService authService;

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
    private ImageUploadService imageStorageService;

    @Transactional
    public Mono<String> uploadProfileImage(Users authUser, FilePart file) {

        if (file == null || !Objects.requireNonNull(file.headers().getContentType()).toString().startsWith("image/")) {
            return Mono.error(new RecordNotFoundException(INVALID_FILE_TYPE));
        }

        if (authUser.getRoles().contains(RoleNames.Candidate.name())) {

            return authService.getMatrimonyDetails(RoleNames.Candidate.name(), authUser).flatMap(res ->
                    matrimonyRepository.findByUserId(authUser.getId()).flatMap(candidate -> {
                        Optional<EventParticipantsResponse> eventPart = res.getEventParticipants().stream().findFirst();
                        String orgId = null;
                        if (eventPart.isPresent()) {
                            orgId = eventPart.get().getOrganizationId();
                        }
                        Image profile = candidate.getProfileImage();
                        if (profile == null) {
                            profile = new Image();
                            candidate.setProfileImage(profile);
                        }
                        String folder = basePath + orgId + candidateImagePath + authUser.getId();
                        // Delete existing image if present
                        return deleteExistingImage(profile)
                                .then(imageStorageService.upload(file, candidate.getId().toHexString(), folder))
                                .flatMap(updatedProfile -> {
                                    candidate.setProfileImage(updatedProfile);
                                    return matrimonyRepository.save(candidate)
                                            .map(savedCandidate -> imageStorageService.getFullImageUrl(savedCandidate.getProfileImage()));
                                });
                    }));
        } else if (authUser.getRoles().contains(RoleNames.Agent.name())) {
            return agentRepository.findByUserId(authUser.getId()).flatMap(agents -> {
                Image profile = agents.getProfileImage();
                if (profile == null) {
                    profile = new Image();
                    agents.setProfileImage(profile);
                }
                String folder = basePath + agents.getOrganizationId() + agentImagePath + authUser.getId();
                // Delete existing image if present
                return deleteExistingImage(profile)
                        .then(imageStorageService.upload(file, agents.getId().toHexString(), folder))
                        .flatMap(updatedProfile -> {
                            agents.setProfileImage(updatedProfile);
                            return agentRepository.save(agents)
                                    .map(savedAgent -> imageStorageService.getFullImageUrl(savedAgent.getProfileImage()));
                        });
            });
        } else if (authUser.getRoles().contains(RoleNames.Organization.name())) {
            return orgRepository.findByUserId(authUser.getId()).flatMap(org -> {
                Image profile = org.getProfileImage();
                if (profile == null) {
                    profile = new Image();
                    org.setProfileImage(profile);
                }
                String folder = basePath + org.getId() + organizationImagePath + authUser.getId();
                return deleteExistingImage(profile)
                        .then(imageStorageService.upload(file, org.getId().toHexString(), folder))
                        .flatMap(updatedProfile -> {
                            org.setProfileImage(updatedProfile);
                            return orgRepository.save(org)
                                    .map(savedOrg -> imageStorageService.getFullImageUrl(savedOrg.getProfileImage()));
                        });
            });
        } else {
            return Mono.error(new RecordNotFoundException("Unsupported user role for profile image upload"));
        }
    }

    public Mono<Void> removeProfileImage(Users authUser) {
        if (authUser.getRoles().contains(RoleNames.Candidate.name())) {
            return matrimonyRepository.findByUserId(authUser.getId()).flatMap(candidate -> {
                Image profile = candidate.getProfileImage();
                return deleteExistingImage(profile)
                        .then(Mono.fromCallable(() -> {
                            candidate.setProfileImage(null);
                            return candidate;
                        }))
                        .flatMap(matrimonyRepository::save)
                        .then();
            });
        } else if (authUser.getRoles().contains(RoleNames.Agent.name())) {
            return agentRepository.findByUserId(authUser.getId()).flatMap(agents -> {
                Image profile = agents.getProfileImage();
                return deleteExistingImage(profile)
                        .then(Mono.fromCallable(() -> {
                            agents.setProfileImage(null);
                            return agents;
                        }))
                        .flatMap(agentRepository::save)
                        .then();
            });
        } else if (authUser.getRoles().contains(RoleNames.Organization.name())) {
            return orgRepository.findByUserId(authUser.getId()).flatMap(org -> {
                Image profile = org.getProfileImage();
                return deleteExistingImage(profile)
                        .then(Mono.fromCallable(() -> {
                            org.setProfileImage(null);
                            return org;
                        }))
                        .flatMap(orgRepository::save)
                        .then();
            });
        } else {
            return Mono.error(new RecordNotFoundException("Unsupported user role for profile image removal"));
        }
    }

    @Transactional
    public Flux<Image> uploadMatrimonyImages(Flux<FilePart> files, Users authUser) {

        return authService.getMatrimonyDetails(RoleNames.Candidate.name(), authUser)
                .flatMapMany(res -> {

                    String orgId = res.getEventParticipants().stream().findFirst().map(EventParticipantsResponse::getOrganizationId)
                            .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND));

                    return matrimonyRepository.findByUserId(authUser.getId())
                            .switchIfEmpty(Mono.error(new RecordNotFoundException("Candidate profile not found")))
                            .flatMapMany(candidate -> {
                                String folder = basePath + orgId + candidateImagePath + authUser.getId() + "/gallery";

                                return files.flatMap(file ->
                                                imageStorageService.upload(file, candidate.getId().toHexString(), folder)
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


    public Mono<String> removeMatrimonyImages(Flux<Image> images, Users authUser) {
        return images.flatMap(image -> matrimonyRepository.findByUserId(authUser.getId())
                .flatMap(candidate -> deleteExistingImage(image)
                        .then(Mono.fromCallable(() -> {
                            if (candidate.getImages() == null || !candidate.getImages().contains(image)) {
                                Mono.just(IMAGES_REMOVED);
                            }
                            candidate.getImages().remove(image);
                            return candidate;
                        }))
                        .flatMap(matrimonyRepository::save)
                        .then())).then(Mono.just(IMAGES_REMOVED));
    }

    private Mono<Void> deleteExistingImage(Image profile) {
        if (profile != null &&
                profile.getId() != null) {
            return imageStorageService.delete(profile.getId());
        }
        return Mono.empty();
    }
}
