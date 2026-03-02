package com.bandhanbook.app.conrollers;

import com.bandhanbook.app.config.MessageUtil;
import com.bandhanbook.app.config.currentUserConfig.CurrentUser;
import com.bandhanbook.app.model.Image;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.payload.request.CandidateRequest;
import com.bandhanbook.app.payload.request.FavoritesRequest;
import com.bandhanbook.app.payload.request.OrganizationRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.*;
import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.service.CommonService;
import com.bandhanbook.app.service.ImageUploadService;
import com.bandhanbook.app.service.ProfileService;
import com.bandhanbook.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "User & Candidate",
        description = "APIs for user registration as candidate, update and delete"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final CommonService commonService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final ProfileService profileService;
    private final ImageUploadService ImageUploadService;
    private final MessageUtil messageUtil;
    private final String OTP_SENT = "OTP sent successfully";

    @Operation(summary = "Register a new Candidate", description = "Registers a new user with the provided details.")
    @PostMapping({"/signup", "/register"})
    public Mono<ResponseEntity<ApiResponse<String>>> register(@Valid @RequestBody UserRegisterRequest userRegisterRequest, @CurrentUser Users authUser) {
        return userService.register(userRegisterRequest, authUser).map(message -> ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message(message)
                        .isOtp(message.equalsIgnoreCase(OTP_SENT))
                        .build()
        ));
    }

    @Operation(summary = "Fetch a Candidate by id", description = "Retrieves the details of a candidate using their unique identifier.")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<CandidateResponse>>> showCandidate(@PathVariable String id, @CurrentUser Users authUser) {
        return userService.showCandidates(id, authUser)
                .map(response -> {
                    if (response.getMatrimony_data() != null &&
                            response.getMatrimony_data().getEvent_participant() != null) {
                        userService.maskPII(response, authUser);
                        response.getMatrimony_data()
                                .getEvent_participant()
                                .forEach(eventParticipant -> {
                                    AgentResponse agent = eventParticipant.getAgent_details();
                                    if (agent != null) {
                                        agent.setLocalAddress(
                                                commonService.getAddressByIds(
                                                        agent.getAddress(),
                                                        agent.getCountry(),
                                                        agent.getState(),
                                                        agent.getCity(),
                                                        agent.getZip()
                                                )
                                        );
                                    }
                                });
                    }

                    return response; // IMPORTANT
                })
                .map(response -> ResponseEntity.ok(
                        ApiResponse.<CandidateResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("records.found"))
                                .data(response)
                                .build()
                ));
    }

    @Operation(summary = "Login from web application")
    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<PhoneLoginResponse>>> myProfile(@CurrentUser Users user) {
        return userService.myProfile(user)
                .map(res -> ResponseEntity.ok(ApiResponse.<PhoneLoginResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("records.found"))
                        .data(res)
                        .build()
                ));
    }

    @GetMapping("")
    public Mono<ResponseEntity<ApiResponse<List<CandidateResponse>>>> listCandidates(@CurrentUser Users authUser, @RequestParam Map<String, String> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
        return userService.listCandidates(authUser, params, page, limit).map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<MatrimonyCandidateResponse>>> updateCandidate(@PathVariable String id, @RequestBody CandidateRequest request, @CurrentUser Users authUser) {
        return userService.updateCandidate(id, request, authUser)
                .map(res -> ResponseEntity.ok(
                        ApiResponse.<MatrimonyCandidateResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("candidate.updated"))
                                .data(res)
                                .build()
                ));
    }

    @Operation(summary = "Add or Remove candidate from favorites", description = "Candidate can have favorite candidate list.")
    @PostMapping({"/favorites"})
    public Mono<ResponseEntity<ApiResponse<String>>> addRemoveToFavorites(@RequestBody FavoritesRequest request, @CurrentUser Users authUser) {
        return userService.addRemoveToFavorites(request.getProfileId(), authUser).map(res -> ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message(res.getMessage())
                        .isFavorite(res.isFavorite())
                        .build()
        ));
    }

    @Operation(summary = "Get favorite candidates of logged in user", description = "Fetch favorite candidates of logged in user.")
    @GetMapping("/favorites")
    public Mono<ResponseEntity<ApiResponse<List<PhoneLoginResponse>>>> getFavorites(@CurrentUser Users user) {
        return userService.getFavorites(user)
                .map(res -> ResponseEntity.ok(ApiResponse.<List<PhoneLoginResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("records.found"))
                        .data(res)
                        .build()
                ));
    }

    @Operation(summary = "Update the organization", description = "Update organization profile")
    @PostMapping({"/me"})
    public Mono<ResponseEntity<ApiResponse<String>>> updateProfile(@Valid @RequestBody OrganizationRequest request, @CurrentUser Users authUser) {
        return userService.updateProfile(request, authUser).map(message -> ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message(message)
                        .build()
        ));
    }

    @DeleteMapping("/me")
    public Mono<ResponseEntity<ApiResponse<String>>> deactivateAccount(@CurrentUser Users authUser) {
        return userService.deactivateAccount(authUser)
                .thenReturn(
                        ResponseEntity.ok(
                                ApiResponse.<String>builder()
                                        .status(HttpStatus.OK.value())
                                        .message(messageUtil.get("account.removed"))
                                        .build()
                        )
                );
    }

    @Operation(summary = "Upload profile image", description = "User can upload profile image.")
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> uploadProfileImage(
            @CurrentUser Users authUser,
            @RequestPart("file") FilePart file
    ) {
        return profileService.uploadProfileImage(authUser, file)
                .map(imageUrl -> ResponseEntity.ok(
                        ApiResponse.<Map<String, String>>builder()
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("profile.updated"))
                                .data(Map.of("image", imageUrl))
                                .build()
                ));
    }

    @Operation(summary = "Remove Profile Image ", description = "User can remove profile image.")
    @DeleteMapping("/profile-image")
    public Mono<ApiResponse<String>> removeProfileImage(@CurrentUser Users authUser) {
        return profileService.removeProfileImage(authUser)
                .thenReturn(
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("profile.image.removed"))
                                .build()
                );
    }

    @Operation(summary = "Upload Candidate images", description = "Candidate can upload multiple images for matrimony profile.")
    @PostMapping(value = "/matrimony-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<List<Image>>> uploadMatrimonyImages(@RequestPart("files") Flux<FilePart> files, @CurrentUser Users authUser) {
        return profileService.uploadMatrimonyImages(files, authUser).map(image -> {
                    image.setUrl(ImageUploadService.getFullImageUrl(image));
                    return image;
                })
                .collectList()
                .map(list -> ApiResponse.<List<Image>>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("image.uploaded"))
                        .data(list)
                        .build()
                );
    }

    @Operation(
            summary = "Remove matrimony image",
            description = "Candidate can remove an image from matrimony profile gallery"
    )
    @DeleteMapping("/matrimony-image/{id}")
    public Mono<ApiResponse<String>> removeMatrimonyImages(@PathVariable String id, @CurrentUser Users authUser) {
        return profileService.removeMatrimonyImages(id, authUser)
                .thenReturn(
                        ApiResponse.<String>builder()
                                .status(HttpStatus.OK.value())
                                .message(messageUtil.get("image.removed"))
                                .build()
                );
    }

    @Operation(summary = "Login from web application")
    @GetMapping("/support")
    public Mono<ResponseEntity<ApiResponse<List<SupportResponse>>>> support(@CurrentUser Users user) {
        return userService.candidateSupport(user)
                .map(res -> ResponseEntity.ok(ApiResponse.<List<SupportResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message(messageUtil.get("records.found"))
                        .data(res)
                        .build()
                ));
    }
}