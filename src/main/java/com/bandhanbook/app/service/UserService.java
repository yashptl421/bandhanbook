package com.bandhanbook.app.service;

import com.bandhanbook.app.exception.EmailNotFoundException;
import com.bandhanbook.app.exception.PhoneNumberNotFoundException;
import com.bandhanbook.app.model.Users;
import com.bandhanbook.app.model.constants.RoleNames;
import com.bandhanbook.app.payload.request.LoginRequest;
import com.bandhanbook.app.payload.request.UserRegisterRequest;
import com.bandhanbook.app.payload.response.LoginResponse;
import com.bandhanbook.app.repository.UserRepository;
import com.bandhanbook.app.security.jwt.JwtService;
import com.bandhanbook.app.security.userprinciple.UserDetailService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.INVALID_CREDENTIALS;
import static com.bandhanbook.app.utilities.ErrorResponseMessages.PHONE_EXISTS;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    UserDetailService userDetailService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ModelMapper modelMapper;

    public Users getUsers() {
        return new Users();
    }

    public Mono<Void> registerUser(UserRegisterRequest request) {
        String role = RoleNames.SuperUser.name();
        return userRepository
                .existsByPhoneNumberAndRole(request.getPhoneNumber(), role)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new PhoneNumberNotFoundException(PHONE_EXISTS));
                    }
                    return Mono.empty();
                }).then(Mono.defer(() -> {
                    Users user = modelMapper.map(request, Users.class);
                    user.setRole(role);
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    return userRepository.save(user);
                })).then();
    }

    public Mono<LoginResponse> webLogin(LoginRequest loginRequest) {

        return userDetailService.findByEmail(loginRequest.getEmail())
                .switchIfEmpty(Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS)))
                .flatMap(user -> {

                    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new EmailNotFoundException(INVALID_CREDENTIALS));
                    }
                    LoginResponse loginResponse = modelMapper.map(user, LoginResponse.class);
                    return Mono.just(LoginResponse.builder()
                            .email(user.getUsers().getEmail())
                            .id(user.getUsers().getId())
                            .role(user.getUsers().getRole())
                            .phoneNumber(user.getUsers().getPhoneNumber())
                            .accessToken(jwtService.generateToken(user))
                            .fullName(user.getUsers().getFullName()).build());
                });
    }

   /* @Transactional
    public Mono<Users> register(UserRegisterRequest userRegisterRequest) {


        return Mono.defer(() -> {
            String role = RoleNames.CANDIDATE.name();
            Optional<List<MatrimonyCandidate>> optCandidates = Optional.empty();

            //get User(_id) from users by phone_number and role
            Optional<Users> userByPhoneNumberAndRole = userRepository.getUserByPhoneNumberAndRole(userRegisterRequest.getPhoneNumber(), role);

            //get candidatesList list from matrimony profiles by user_id
            if (userByPhoneNumberAndRole.isPresent()) {
                optCandidates = matrimonyRepository.getCandidatesByUserId(userByPhoneNumberAndRole.get().getId());
            }

            //check candidate exist by eventId in EventParticipant
            boolean candidateExist = false;
            if (optCandidates.isPresent()) {
                List<MatrimonyCandidate> candidateList = optCandidates.get();
                candidateExist = candidateList.stream().anyMatch(candidate ->
                        eventParticipantsRepository.existsCandidateByEventId(candidate.getId(), userRegisterRequest.getEventId()));
            }
            if (candidateExist)
                return Mono.error(new PhoneNumberNotFoundException(ErrorResponse.PHONE_EXISTS));

            UserTokens userTokens;
            //validate tpo if exist
            if (userRegisterRequest.getOtp() != null) {
                userTokens = userTokensRepository.findByPhoneRoleAndOtp(userRegisterRequest.getPhoneNumber(), role, userRegisterRequest.getOtp());
                return Mono.error(new InvalidOtpException(ErrorResponse.INVALID_OTP));
            }


            Users user = userRepository.save(registerReqToUsers(userRegisterRequest, role));
            userTokensRepository.deleteByPhoneNumber(userRegisterRequest.getPhoneNumber());
            matrimonyRepository.save(registerReqToCandidate(userRegisterRequest, user));

            return Mono.just(user);
        });
    }*/
      /*  return Mono.defer(() -> {
            if (existsByUsername(registerUserRequest.getUsername())) {
                return Mono.error(new EmailOrUsernameNotFoundException("The username " + registerUserRequest.getUsername() + " is existed, please try again."));
            }
            if (existsByEmail(registerUserRequest.getEmail())) {
                return Mono.error(new EmailOrUsernameNotFoundException("The email " + registerUserRequest.getEmail() + " is existed, please try again."));
            }
            if (existsByPhoneNumber(registerUserRequest.getPhone())) {
                return Mono.error(new PhoneNumberNotFoundException("The phone number " + registerUserRequest.getPhone() + " is existed, please try again."));
            }

            User user = modelMapper.map(registerUserRequest, User.class);
            user.setPassword(passwordEncoder.encode(registerUserRequest.getPassword()));
            user.setRoles(registerUserRequest.getRoles()
                    .stream()
                    .map(role -> roleService.findByName(mapToRoleName(role))
                            .orElseThrow(() -> new RuntimeException("Role not found in the database.")))
                    .collect(Collectors.toSet()));

            userRepository.save(user);
            return Mono.just(user);
        });*/


   /* private Users registerReqToUsers(UserRegisterRequest userRegisterRequest, String role) {
        return Users.builder()
                .phoneNumber(userRegisterRequest.getPhoneNumber())
                .fullName(userRegisterRequest.getFullName())
                .email(userRegisterRequest.getEmail())
                .build();
    }

    private MatrimonyCandidate registerReqToCandidate(UserRegisterRequest userRegisterRequest, Users user) {
        return MatrimonyCandidate.builder().userId(user.getId())
                .personalDetails(MatrimonyCandidate.PersonalDetails.builder()
                        .dob(userRegisterRequest.getDob())
                        .gender(userRegisterRequest.getGender())
                        .build())
                .address(
                        MatrimonyCandidate.Address.builder()
                                .address(userRegisterRequest.getAddress())
                                .zip(userRegisterRequest.getZip())
                                .city(userRegisterRequest.getCity())
                                .state(userRegisterRequest.getState()).build())

                .build();
    }*/
}
