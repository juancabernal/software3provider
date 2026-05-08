package com.co.eatupapi.services.commercial.seller.impl;

import com.co.eatupapi.domain.commercial.seller.SellerDomain;
import com.co.eatupapi.domain.commercial.seller.SellerStatus;
import com.co.eatupapi.dto.commercial.seller.SellerDTO;
import com.co.eatupapi.dto.commercial.seller.SellerPatchDTO;
import com.co.eatupapi.messaging.commercial.seller.SellerEventPublisher;
import com.co.eatupapi.repositories.commercial.seller.SellerRepository;
import com.co.eatupapi.services.commercial.seller.SellerService;
import com.co.eatupapi.utils.commercial.seller.exceptions.SellerBusinessException;
import com.co.eatupapi.utils.commercial.seller.exceptions.SellerNotFoundException;
import com.co.eatupapi.utils.commercial.seller.exceptions.SellerValidationException;
import com.co.eatupapi.utils.commercial.seller.mapper.SellerMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SellerServiceImpl implements SellerService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$");
    private static final double MAX_COMMISSION = 30.0;
    private static final double MIN_COMMISSION = 0.0;
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";

    private final SellerRepository sellerRepository;
    private final SellerMapper sellerMapper;
    private final SellerEventPublisher sellerEventPublisher;

    public SellerServiceImpl(
            SellerRepository sellerRepository,
            SellerMapper sellerMapper,
            SellerEventPublisher sellerEventPublisher
    ) {
        this.sellerRepository = sellerRepository;
        this.sellerMapper = sellerMapper;
        this.sellerEventPublisher = sellerEventPublisher;
    }

    // ── COMMANDS (ESCRITURA - SOLO EVENTOS A RABBITMQ) ───────────────────────────

    @Override
    public SellerDTO createSeller(SellerDTO request) {
        if (request == null) {
            throw new SellerValidationException("Request body is required");
        }

        validateSellerPayload(request);
        validateDuplicateEmail(request.getEmail());
        validateDuplicateIdentification(request.getIdentificationNumber());
        validateDuplicatePhone(request.getPhone());

        LocalDateTime now = LocalDateTime.now();

        SellerDomain sellerDomain = sellerMapper.toDomain(request);
        sellerDomain.setId(UUID.randomUUID());
        sellerDomain.setDocumentTypeId(request.getDocumentTypeId());
        sellerDomain.setLocationId(request.getLocationId());
        sellerDomain.setFirstName(request.getFirstName().trim());
        sellerDomain.setLastName(request.getLastName().trim());
        sellerDomain.setIdentificationNumber(request.getIdentificationNumber().trim());
        sellerDomain.setPhone(request.getPhone().trim());
        sellerDomain.setEmail(request.getEmail().trim().toLowerCase());
        sellerDomain.setCommissionPercentage(request.getCommissionPercentage());
        sellerDomain.setStatus(SellerStatus.ACTIVE);
        sellerDomain.setCreatedDate(now);
        sellerDomain.setModifiedDate(now);

        SellerDTO payload = sellerMapper.toDto(sellerDomain);
        sellerEventPublisher.publishSellerCreated(payload);
        return payload;
    }

    @Override
    public SellerDTO updateSeller(UUID sellerId, SellerDTO request) {
        validateId(sellerId, "sellerId");
        if (request == null) {
            throw new SellerValidationException("Request body is required");
        }

        validateSellerPayload(request);

        SellerDomain existing = findSellerById(sellerId);
        validateImmutableEmail(existing.getEmail(), request.getEmail().trim().toLowerCase());
        validateDuplicateIdentificationOnUpdate(request.getIdentificationNumber(), sellerId);
        validateDuplicatePhoneOnUpdate(request.getPhone(), sellerId);

        existing.setDocumentTypeId(request.getDocumentTypeId());
        existing.setLocationId(request.getLocationId());
        existing.setIdentificationNumber(request.getIdentificationNumber().trim());
        existing.setFirstName(request.getFirstName().trim());
        existing.setLastName(request.getLastName().trim());
        existing.setPhone(request.getPhone().trim());
        existing.setCommissionPercentage(request.getCommissionPercentage());
        existing.setEmail(request.getEmail().trim().toLowerCase());
        existing.setModifiedDate(LocalDateTime.now());

        SellerDTO payload = sellerMapper.toDto(existing);
        sellerEventPublisher.publishSellerUpdated(sellerId.toString(), payload);
        return payload;
    }

    @Override
    public SellerDTO updateStatus(UUID sellerId, String status) {
        validateId(sellerId, "sellerId");
        SellerStatus newStatus = parseRequiredStatus(status);

        SellerDomain existing = findSellerById(sellerId);
        existing.setStatus(newStatus);
        existing.setModifiedDate(LocalDateTime.now());

        SellerDTO payload = sellerMapper.toDto(existing);
        sellerEventPublisher.publishSellerStatusUpdated(sellerId.toString(), newStatus.name());
        return payload;
    }

    @Override
    public SellerDTO patchSeller(UUID sellerId, SellerPatchDTO request) {
        validateId(sellerId, "sellerId");
        if (request == null) {
            throw new SellerValidationException("Request body is required");
        }

        SellerDomain existing = findSellerById(sellerId);

        if (request.getFirstName() != null) {
            validateRequiredText(request.getFirstName(), FIELD_FIRST_NAME);
            validateName(request.getFirstName(), FIELD_FIRST_NAME);
            existing.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            validateRequiredText(request.getLastName(), FIELD_LAST_NAME);
            validateName(request.getLastName(), FIELD_LAST_NAME);
            existing.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            validateRequiredText(request.getPhone(), "phone");
            validatePhone(request.getPhone());
            validateDuplicatePhoneOnUpdate(request.getPhone(), sellerId);
            existing.setPhone(request.getPhone().trim());
        }
        if (request.getCommissionPercentage() != null) {
            validateCommissionPercentage(request.getCommissionPercentage());
            existing.setCommissionPercentage(request.getCommissionPercentage());
        }
        if (request.getIdentificationNumber() != null) {
            validateRequiredText(request.getIdentificationNumber(), "identificationNumber");
            validateIdentificationNumber(request.getIdentificationNumber());
            validateDuplicateIdentificationOnUpdate(request.getIdentificationNumber(), sellerId);
            existing.setIdentificationNumber(request.getIdentificationNumber().trim());
        }
        if (request.getLocationId() != null) {
            existing.setLocationId(request.getLocationId());
        }
        if (request.getDocumentTypeId() != null) {
            existing.setDocumentTypeId(request.getDocumentTypeId());
        }

        existing.setModifiedDate(LocalDateTime.now());

        SellerPatchDTO patchPayload = new SellerPatchDTO();
        patchPayload.setFirstName(existing.getFirstName());
        patchPayload.setLastName(existing.getLastName());
        patchPayload.setPhone(existing.getPhone());
        patchPayload.setCommissionPercentage(existing.getCommissionPercentage());
        patchPayload.setIdentificationNumber(existing.getIdentificationNumber());
        patchPayload.setLocationId(existing.getLocationId());
        patchPayload.setDocumentTypeId(existing.getDocumentTypeId());

        sellerEventPublisher.publishSellerPatched(sellerId.toString(), patchPayload);
        return sellerMapper.toDto(existing);
    }

    // ── GETS (LECTURA - DB REAL) ────────────────────────────────────────────────

    @Override
    public SellerDTO getSellerById(UUID sellerId) {
        validateId(sellerId, "sellerId");
        return sellerMapper.toDto(findSellerById(sellerId));
    }

    @Override
    public List<SellerDTO> getSellers(String status) {
        List<SellerDomain> result;
        if (status == null || status.isBlank()) {
            result = sellerRepository.findAll();
        } else {
            SellerStatus parsedStatus = parseStatus(status);
            result = sellerRepository.findByStatus(parsedStatus);
        }
        return result.stream()
                .sorted((a, b) -> a.getCreatedDate().compareTo(b.getCreatedDate()))
                .map(sellerMapper::toDto)
                .toList();
    }

    private void validateId(UUID value, String fieldName) {
        if (value == null) {
            throw new SellerValidationException("Field " + fieldName + " is required and cannot be empty");
        }
    }

    private SellerDomain findSellerById(UUID sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found with id: " + sellerId));
    }

    private SellerStatus parseStatus(String status) {
        try {
            return SellerStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new SellerValidationException("Invalid seller status value");
        }
    }

    private SellerStatus parseRequiredStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new SellerValidationException("Seller status is required");
        }
        return parseStatus(status);
    }

    private void validateSellerPayload(SellerDTO request) {
        validateRequiredObject(request.getDocumentTypeId(), "documentTypeId");
        validateRequiredObject(request.getLocationId(), "locationId");
        validateRequiredText(request.getIdentificationNumber(), "identificationNumber");
        validateRequiredText(request.getFirstName(), FIELD_FIRST_NAME);
        validateRequiredText(request.getLastName(), FIELD_LAST_NAME);
        validateRequiredText(request.getPhone(), "phone");
        validateRequiredText(request.getEmail(), "email");
        validateRequiredObject(request.getCommissionPercentage(), "commissionPercentage");

        validateEmail(request.getEmail());
        validatePhone(request.getPhone());
        validateCommissionPercentage(request.getCommissionPercentage());
        validateIdentificationNumber(request.getIdentificationNumber());
        validateName(request.getFirstName(), FIELD_FIRST_NAME);
        validateName(request.getLastName(), FIELD_LAST_NAME);
    }

    private void validateRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new SellerValidationException("Field '" + fieldName + "' is required and cannot be empty");
        }
    }

    private void validateRequiredObject(Object value, String fieldName) {
        if (value == null) {
            throw new SellerValidationException("Field '" + fieldName + "' is required and cannot be empty");
        }
    }

    private void validateIdentificationNumber(String number) {
        if (!DIGITS_PATTERN.matcher(number.trim()).matches()) {
            throw new SellerValidationException("Identification number must contain only digits");
        }
        if (number.trim().length() < 6 || number.trim().length() > 20) {
            throw new SellerValidationException("Identification number must be between 6 and 20 digits");
        }
    }

    private void validateName(String value, String fieldName) {
        if (!NAME_PATTERN.matcher(value.trim()).matches()) {
            throw new SellerValidationException("Field '" + fieldName + "' must contain only letters");
        }
        if (value.trim().length() > 100) {
            throw new SellerValidationException("Field '" + fieldName + "' must not exceed 100 characters");
        }
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new SellerValidationException(
                    "Invalid email format: '" + email + "'. Expected format: example@domain.com");
        }
    }

    private void validatePhone(String phone) {
        if (!DIGITS_PATTERN.matcher(phone.trim()).matches()) {
            throw new SellerValidationException("Phone number must contain only digits");
        }
        if (phone.trim().length() != 10) {
            throw new SellerValidationException("Phone number must contain exactly 10 digits");
        }
    }

    private void validateDuplicatePhone(String phone) {
        if (sellerRepository.existsByPhone(phone.trim())) {
            throw new SellerBusinessException(
                    "A seller with phone '" + phone + "' already exists");
        }
    }

    private void validateDuplicatePhoneOnUpdate(String phone, UUID currentSellerId) {
        if (sellerRepository.existsByPhoneAndIdNot(phone.trim(), currentSellerId)) {
            throw new SellerBusinessException(
                    "A seller with phone '" + phone + "' already exists");
        }
    }

    private void validateCommissionPercentage(Double commission) {
        if (commission < MIN_COMMISSION) {
            throw new SellerValidationException("Commission percentage cannot be negative");
        }
        if (commission > MAX_COMMISSION) {
            throw new SellerValidationException("Commission percentage cannot exceed 30%");
        }
        BigDecimal bd = BigDecimal.valueOf(commission).stripTrailingZeros();
        if (bd.scale() > 2) {
            throw new SellerValidationException("Commission percentage must have at most 2 decimal places");
        }
    }

    private void validateDuplicateEmail(String email) {
        if (sellerRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new SellerBusinessException("A seller with email '" + email + "' already exists");
        }
    }

    private void validateDuplicateIdentification(String identificationNumber) {
        if (sellerRepository.existsByIdentificationNumber(identificationNumber.trim())) {
            throw new SellerBusinessException(
                    "A seller with identification number '" + identificationNumber + "' already exists");
        }
    }

    private void validateDuplicateIdentificationOnUpdate(String identificationNumber, UUID currentSellerId) {
        if (sellerRepository.existsByIdentificationNumberAndIdNot(identificationNumber.trim(), currentSellerId)) {
            throw new SellerBusinessException(
                    "A seller with identification number '" + identificationNumber + "' already exists");
        }
    }

    private void validateImmutableEmail(String currentEmail, String requestedEmail) {
        if (!currentEmail.equals(requestedEmail)) {
            throw new SellerBusinessException(
                    "Email address cannot be modified once the seller has been created");
        }
    }
}