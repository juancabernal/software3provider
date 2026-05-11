package com.co.eatupapi.services.user;

import com.co.eatupapi.domain.user.UserDomain;
import com.co.eatupapi.domain.user.UserStatus;
import com.co.eatupapi.dto.user.CreateUserRequest;
import com.co.eatupapi.dto.user.UpdateUserRequest;
import com.co.eatupapi.dto.user.UserResponse;
import com.co.eatupapi.dto.user.UserSummaryResponse;
import com.co.eatupapi.messaging.user.UserCommandPublisher;
import com.co.eatupapi.repositories.user.UserRepository;
import com.co.eatupapi.utils.user.exceptions.UserBusinessException;
import com.co.eatupapi.utils.user.exceptions.UserNotFoundException;
import com.co.eatupapi.utils.user.exceptions.UserValidationException;
import com.co.eatupapi.utils.user.mapper.UserMapper;
import com.co.eatupapi.utils.user.validation.UserValidationRules;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final String GENERIC_BUSINESS_REJECTION_MESSAGE = "Request could not be processed";
    private static final int MAX_ALLOWED_AGE_YEARS = 120;
    private static final Pattern NAME_PATTERN = Pattern.compile(UserValidationRules.NAME_REGEX);
    private static final Pattern DOCUMENT_NUMBER_PATTERN = Pattern.compile(UserValidationRules.DOCUMENT_NUMBER_REGEX);
    private static final Pattern PHONE_PATTERN = Pattern.compile(UserValidationRules.PHONE_REGEX);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(UserValidationRules.EMAIL_REGEX);
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(UserValidationRules.ADDRESS_REGEX);
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[^A-Za-z\\d\\s].*");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(".*\\s+.*");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CatalogService catalogService;
    private final UserCommandPublisher userCommandPublisher;

    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           CatalogService catalogService,
                           UserCommandPublisher userCommandPublisher) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.catalogService = catalogService;
        this.userCommandPublisher = userCommandPublisher;
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        normalizeCreatePayload(request);
        validateCreatePayload(request);
        validateDuplicateEmail(request.getEmail());
        validateCatalogReferences(request.getDocumentTypeId(), request.getDepartmentId(), request.getCityId());

        UserDomain user = userMapper.toDomain(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        userCommandPublisher.publishCreate(request);
        user.setModifiedAt(LocalDateTime.now());

        UserDomain saved = saveUser(user);
        return enrichResponse(userMapper.toResponse(saved), saved);
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        UserDomain user = findUserById(userId);
        return enrichResponse(userMapper.toResponse(user), user);
    }

    @Override
    public List<UserSummaryResponse> getUsers(String status, Integer page, Integer size) {
        validatePagination(page, size);
        UserStatus userStatus = parseOptionalStatus(status);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UserDomain> usersPage = userStatus == null
                ? userRepository.findAll(pageable)
                : userRepository.findByStatus(userStatus, pageable);

        return usersPage.stream()
                .map(user -> {
                    UserSummaryResponse summary = userMapper.toSummaryResponse(user);
                    summary.setLocation(resolveLocationForResponse(user));
                    return summary;
                })
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        normalizeUpdatePayload(request);
        validateUpdatePayload(request);

        UserDomain existing = findUserById(userId);
        validateCatalogReferences(request.getDocumentTypeId(), request.getDepartmentId(), request.getCityId());

        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setDocumentTypeId(request.getDocumentTypeId());
        existing.setDocumentNumber(request.getDocumentNumber());
        existing.setPhone(request.getPhone());
        existing.setBirthDate(request.getBirthDate());
        existing.setDepartmentId(request.getDepartmentId());
        existing.setCityId(request.getCityId());
        existing.setAddress(request.getAddress());
        existing.setLocationId(request.getLocationId());
        existing.setModifiedAt(LocalDateTime.now());

        UserDomain saved = saveUser(existing);
        userCommandPublisher.publishUpdate(userId, request);
        return enrichResponse(userMapper.toResponse(saved), saved);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID userId, String status) {
        UserStatus newStatus = parseRequiredStatus(status);
        UserDomain existing = findUserById(userId);
        existing.setStatus(newStatus);
        existing.setModifiedAt(LocalDateTime.now());

        UserDomain saved = saveUser(existing);
        userCommandPublisher.publishStatusUpdate(userId, status);
        return enrichResponse(userMapper.toResponse(saved), saved);
    }

    private UserDomain findUserById(UUID userId) {
        if (userId == null) {
            throw new UserValidationException("User id is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void validateCreatePayload(CreateUserRequest request) {
        if (request == null) {
            throw new UserValidationException("Request body is required");
        }

        validateRequiredText(request.getFirstName(), "firstName");
        validateRequiredText(request.getLastName(), "lastName");
        validateRequiredObject(request.getDocumentTypeId(), "documentTypeId");
        validateRequiredText(request.getDocumentNumber(), "documentNumber");
        validateRequiredText(request.getPhone(), "phone");
        validateRequiredText(request.getEmail(), "email");
        validateRequiredText(request.getPassword(), "password");
        validateRequiredObject(request.getBirthDate(), "birthDate");
        validateRequiredObject(request.getDepartmentId(), "departmentId");
        validateRequiredObject(request.getCityId(), "cityId");
        validateRequiredText(request.getAddress(), "address");
        validateRequiredObject(request.getLocationId(), "locationId");

        validateName(request.getFirstName(), "firstName");
        validateName(request.getLastName(), "lastName");
        validateDocumentNumber(request.getDocumentNumber());
        validatePhone(request.getPhone());
        validateEmail(request.getEmail());
        validatePasswordStrength(request.getPassword());
        validateBirthDate(request.getBirthDate());
        validateAddress(request.getAddress());
    }

    private void validateUpdatePayload(UpdateUserRequest request) {
        if (request == null) {
            throw new UserValidationException("Request body is required");
        }

        if (request.getEmail() != null) {
            throw new UserValidationException("Field 'email' cannot be modified");
        }

        validateRequiredText(request.getFirstName(), "firstName");
        validateRequiredText(request.getLastName(), "lastName");
        validateRequiredObject(request.getDocumentTypeId(), "documentTypeId");
        validateRequiredText(request.getDocumentNumber(), "documentNumber");
        validateRequiredText(request.getPhone(), "phone");
        validateRequiredObject(request.getBirthDate(), "birthDate");
        validateRequiredObject(request.getDepartmentId(), "departmentId");
        validateRequiredObject(request.getCityId(), "cityId");
        validateRequiredText(request.getAddress(), "address");
        validateRequiredObject(request.getLocationId(), "locationId");

        validateName(request.getFirstName(), "firstName");
        validateName(request.getLastName(), "lastName");
        validateDocumentNumber(request.getDocumentNumber());
        validatePhone(request.getPhone());
        validateBirthDate(request.getBirthDate());
        validateAddress(request.getAddress());
    }

    private void validatePagination(Integer page, Integer size) {
        if (page == null) {
            throw new UserValidationException("Field 'page' is required");
        }
        if (size == null) {
            throw new UserValidationException("Field 'size' is required");
        }
        if (page < UserValidationRules.PAGE_MIN) {
            throw new UserValidationException("Field 'page' must be greater than or equal to 0");
        }
        if (size < UserValidationRules.SIZE_MIN || size > UserValidationRules.SIZE_MAX) {
            throw new UserValidationException("Field 'size' must be between 1 and 100");
        }
    }

    private void validateRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserValidationException("Field '" + fieldName + "' is required and cannot be empty");
        }
    }

    private void validateRequiredObject(Object value, String fieldName) {
        if (value == null) {
            throw new UserValidationException("Field '" + fieldName + "' is required and cannot be empty");
        }
    }

    private void validateName(String name, String fieldName) {
        if (name.length() < UserValidationRules.NAME_MIN_LENGTH || name.length() > UserValidationRules.NAME_MAX_LENGTH) {
            throw new UserValidationException("Field '" + fieldName + "' must be between 2 and 100 characters");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new UserValidationException("Field '" + fieldName + "' contains invalid characters");
        }
    }

    private void validateDocumentNumber(String documentNumber) {
        if (documentNumber.length() < UserValidationRules.DOCUMENT_NUMBER_MIN_LENGTH
                || documentNumber.length() > UserValidationRules.DOCUMENT_NUMBER_MAX_LENGTH) {
            throw new UserValidationException("Field 'documentNumber' must be between 5 and 30 characters");
        }
        if (!DOCUMENT_NUMBER_PATTERN.matcher(documentNumber).matches()) {
            throw new UserValidationException("Field 'documentNumber' contains invalid characters");
        }
    }

    private void validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new UserValidationException("Phone number must contain exactly 10 digits");
        }
    }

    private void validateEmail(String email) {
        if (email.length() > UserValidationRules.EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new UserValidationException("Invalid email format");
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        if (!birthDate.isBefore(today)) {
            throw new UserValidationException("Birth date must be a past date");
        }
        if (birthDate.isBefore(today.minusYears(MAX_ALLOWED_AGE_YEARS))) {
            throw new UserValidationException("Birth date is out of allowed range");
        }
    }

    private void validateAddress(String address) {
        if (address.length() < UserValidationRules.ADDRESS_MIN_LENGTH
                || address.length() > UserValidationRules.ADDRESS_MAX_LENGTH) {
            throw new UserValidationException("Field 'address' must be between 5 and 255 characters");
        }
        if (!ADDRESS_PATTERN.matcher(address).matches()) {
            throw new UserValidationException("Field 'address' contains invalid characters");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.isBlank()) {
            throw new UserValidationException("Field 'password' is required and cannot be empty");
        }
        if (password.length() < UserValidationRules.PASSWORD_MIN_LENGTH
                || password.length() > UserValidationRules.PASSWORD_MAX_LENGTH) {
            throw new UserValidationException("Password must contain between 8 and 72 characters");
        }
        if (WHITESPACE_PATTERN.matcher(password).matches()) {
            throw new UserValidationException("Password must not contain whitespace characters");
        }
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            throw new UserValidationException("Password must contain at least one uppercase letter");
        }
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            throw new UserValidationException("Password must contain at least one lowercase letter");
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            throw new UserValidationException("Password must contain at least one number");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            throw new UserValidationException("Password must contain at least one special character");
        }
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserBusinessException(GENERIC_BUSINESS_REJECTION_MESSAGE);
        }
    }

    private void validateCatalogReferences(UUID documentTypeId, UUID departmentId, UUID cityId) {
        if (!catalogService.documentTypeExists(documentTypeId)) {
            throw new UserValidationException("Selected document type does not exist");
        }
        if (!catalogService.departmentExists(departmentId)) {
            throw new UserValidationException("Selected department does not exist");
        }
        if (!catalogService.cityExists(cityId)) {
            throw new UserValidationException("Selected city does not exist");
        }
        if (!catalogService.cityBelongsToDepartment(cityId, departmentId)) {
            throw new UserValidationException("Selected city does not belong to the selected department");
        }
    }

    private UserStatus parseRequiredStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new UserValidationException("User status is required");
        }
        return parseStatus(status);
    }

    private UserStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseStatus(status);
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new UserValidationException("Invalid user status value");
        }
    }

    private UserResponse enrichResponse(UserResponse response, UserDomain user) {
        response.setDocumentType(catalogService.getDocumentTypeName(user.getDocumentTypeId()));
        response.setDepartment(catalogService.getDepartmentName(user.getDepartmentId()));
        response.setCity(catalogService.getCityName(user.getCityId()));
        response.setLocation(resolveLocationForResponse(user));
        return response;
    }

    private String resolveLocationForResponse(UserDomain user) {
        return user.getLocationId() != null ? user.getLocationId().toString() : null;
    }

    private UserDomain saveUser(UserDomain user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserBusinessException(GENERIC_BUSINESS_REJECTION_MESSAGE);
        }
    }

    private void normalizeCreatePayload(CreateUserRequest request) {
        if (request == null) {
            return;
        }
        request.setFirstName(normalizeName(request.getFirstName()));
        request.setLastName(normalizeName(request.getLastName()));
        request.setDocumentNumber(normalizeText(request.getDocumentNumber()));
        request.setPhone(normalizeText(request.getPhone()));
        request.setEmail(normalizeEmail(request.getEmail()));
        request.setAddress(normalizeAddress(request.getAddress()));
    }

    private void normalizeUpdatePayload(UpdateUserRequest request) {
        if (request == null) {
            return;
        }
        request.setFirstName(normalizeName(request.getFirstName()));
        request.setLastName(normalizeName(request.getLastName()));
        request.setDocumentNumber(normalizeText(request.getDocumentNumber()));
        request.setPhone(normalizeText(request.getPhone()));
        request.setAddress(normalizeAddress(request.getAddress()));
    }

    private String normalizeName(String value) {
        return normalizeMultiSpaceText(value);
    }

    private String normalizeAddress(String value) {
        return normalizeMultiSpaceText(value);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeMultiSpaceText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
