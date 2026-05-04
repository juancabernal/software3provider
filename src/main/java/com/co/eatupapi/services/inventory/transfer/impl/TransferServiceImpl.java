package com.co.eatupapi.services.inventory.transfer.impl;

import com.co.eatupapi.domain.inventory.product.Product;
import com.co.eatupapi.domain.inventory.transfer.Transfer;
import com.co.eatupapi.domain.inventory.transfer.TransferStatus;
import com.co.eatupapi.domain.user.UserDomain;
import com.co.eatupapi.dto.inventory.transfer.TransferObservacionUpdateDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferRequestDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferResponseDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferStatusUpdateDTO;
import com.co.eatupapi.repositories.inventory.location.LocationRepository;
import com.co.eatupapi.repositories.inventory.product.ProductRepository;
import com.co.eatupapi.repositories.inventory.transfer.TransferRepository;
import com.co.eatupapi.repositories.user.UserRepository;
import com.co.eatupapi.services.inventory.transfer.TransferService;
import com.co.eatupapi.utils.inventory.transfer.exceptions.TransferBusinessException;
import com.co.eatupapi.utils.inventory.transfer.exceptions.TransferNotFoundException;
import com.co.eatupapi.utils.inventory.transfer.exceptions.TransferValidationException;
import com.co.eatupapi.utils.inventory.transfer.mapper.TransferMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class TransferServiceImpl implements TransferService {

    private static final String ORIGIN_ROLE = "origen";
    private static final String DESTINATION_ROLE = "destino";
    private static final String DESTINATION_REQUIRED_MESSAGE = "La sede destino es obligatoria";
    private static final String TRANSFER_NOT_FOUND_MESSAGE = "Transferencia no encontrada con id: ";

    private record TransferProducts(Product originProduct, Product destinationProduct) {
    }

    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public TransferServiceImpl(TransferRepository transferRepository,
                               TransferMapper transferMapper,
                               ProductRepository productRepository,
                               LocationRepository locationRepository,
                               UserRepository userRepository) {
        this.transferRepository = transferRepository;
        this.transferMapper = transferMapper;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TransferResponseDTO create(TransferRequestDTO request) {
        TransferProducts products = validateAndLoadRequest(request);
        validateAuthenticatedUserCanCreateTransfer(request.sedeOrigen());
        Transfer transfer = transferMapper.toDomain(request);
        transfer.setEstado(TransferStatus.EN_PROCESO);
        transfer.setStock(products.originProduct().getStock());
        return transferMapper.toResponse(transferRepository.save(transfer));
    }

    @Override
    @Transactional
    public TransferResponseDTO updateStatus(Long id, String sedeOrigen, TransferStatusUpdateDTO statusUpdate) {
        validateId(id);
        if (statusUpdate == null || statusUpdate.estado() == null) {
            throw new TransferValidationException("El estado es obligatorio");
        }

        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(TRANSFER_NOT_FOUND_MESSAGE + id));

        syncTransitStatus(transfer);
        if (statusUpdate.estado() == TransferStatus.CANCELADO) {
            validateCancellationAllowed(transfer, sedeOrigen);
        }
        if (statusUpdate.estado() == TransferStatus.EN_TRANSITO) {
            validateTransitAllowed(transfer, sedeOrigen);
        }
        validateManualStatusUpdate(statusUpdate.estado());
        validateStatusTransition(transfer.getEstado(), statusUpdate.estado());
        if (statusUpdate.estado() == TransferStatus.COMPLETADO) {
            applyInventoryMovement(transfer);
        }
        transfer.setEstado(statusUpdate.estado());
        return transferMapper.toResponse(transferRepository.save(transfer));
    }

    @Override
    public TransferResponseDTO findById(Long id) {
        validateId(id);
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(TRANSFER_NOT_FOUND_MESSAGE + id));
        syncTransitStatus(transfer);
        return transferMapper.toResponse(transfer);
    }

    @Override
    public List<TransferResponseDTO> findAll() {
        return transferRepository.findAll()
                .stream()
                .map(this::syncTransitStatus)
                .map(transferMapper::toResponse)
                .toList();
    }

    @Override
    public List<TransferResponseDTO> findAllInTransit() {
        return transferRepository.findAll()
                .stream()
                .map(this::syncTransitStatus)
                .filter(transfer -> transfer.getEstado() == TransferStatus.EN_TRANSITO)
                .map(transferMapper::toResponse)
                .toList();
    }

    @Override
    public List<TransferResponseDTO> findAllCompleted() {
        return transferRepository.findByEstado(TransferStatus.COMPLETADO)
                .stream()
                .map(transferMapper::toResponse)
                .toList();
    }

    @Override
    public List<TransferResponseDTO> findAllCancelled() {
        return transferRepository.findByEstado(TransferStatus.CANCELADO)
                .stream()
                .map(transferMapper::toResponse)
                .toList();
    }

    @Override
    public List<TransferResponseDTO> findIncoming(String sedeDestino) {
        validateRequiredLocationId(sedeDestino, DESTINATION_REQUIRED_MESSAGE, DESTINATION_ROLE);
        return transferRepository.findBySedeDestino(sedeDestino.trim())
                .stream()
                .map(this::syncTransitStatus)
                .map(transferMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TransferResponseDTO confirmReceipt(Long id, String sedeDestino) {
        Transfer transfer = getDestinationTransfer(id, sedeDestino);
        validateReceivableStatus(transfer);
        applyInventoryMovement(transfer);
        transfer.setEstado(TransferStatus.COMPLETADO);
        return transferMapper.toResponse(transferRepository.save(transfer));
    }

    @Override
    @Transactional
    public TransferResponseDTO claimReceipt(Long id, String sedeDestino, TransferObservacionUpdateDTO observacionUpdate) {
        Transfer transfer = getDestinationTransfer(id, sedeDestino);
        validateReceivableStatus(transfer);

        String observations = observacionUpdate == null ? null : observacionUpdate.observaciones();
        if (observations == null || observations.isBlank()) {
            throw new TransferValidationException("La observación del reclamo es obligatoria");
        }

        transfer.setObservaciones(observations.trim());
        transfer.setEstado(TransferStatus.RECLAMADO);
        return transferMapper.toResponse(transferRepository.save(transfer));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new TransferValidationException("El id de la transferencia es obligatorio y debe ser mayor a cero");
        }
    }

    private void validateRequest(TransferRequestDTO request) {
        if (request == null) {
            throw new TransferValidationException("La solicitud no puede estar vacía");
        }
        validateLocations(request);
        validateCoreFields(request);
        validateDates(request);
        validateQuantity(request);
    }

    private TransferProducts validateAndLoadRequest(TransferRequestDTO request) {
        validateRequest(request);
        validateLocationExistsAndActive(request.sedeOrigen(), ORIGIN_ROLE);
        validateLocationExistsAndActive(request.sedeDestino(), DESTINATION_ROLE);

        Product originProduct = findProductByNameAndLocation(request.producto(), request.sedeOrigen(), ORIGIN_ROLE);
        Product destinationProduct = findProductByNameAndLocation(request.producto(), request.sedeDestino(), DESTINATION_ROLE);
        validateStockAndQuantity(originProduct.getStock(), request.cantidad());
        return new TransferProducts(originProduct, destinationProduct);
    }

    private void validateLocations(TransferRequestDTO request) {
        if (request.sedeOrigen() == null || request.sedeOrigen().isBlank()) {
            throw new TransferValidationException("La sede de origen es obligatoria y debe ser válida");
        }
        if (request.sedeDestino() == null || request.sedeDestino().isBlank()) {
            throw new TransferValidationException("La sede de destino es obligatoria y debe ser válida");
        }
        if (request.sedeOrigen().trim().equals(request.sedeDestino().trim())) {
            throw new TransferBusinessException("La sede de origen no puede ser igual a la sede de destino");
        }
    }

    private void validateCoreFields(TransferRequestDTO request) {
        if (request.fechaEnvio() == null) {
            throw new TransferValidationException("La fecha de transferencia es obligatoria");
        }
        if (request.fechaLlegada() == null) {
            throw new TransferValidationException("La fecha de llegada es obligatoria");
        }
        if (request.responsable() == null || request.responsable().isBlank()) {
            throw new TransferValidationException("El responsable es obligatorio");
        }
        if (request.producto() == null || request.producto().isBlank()) {
            throw new TransferValidationException("El producto es obligatorio y debe ser válido");
        }
    }

    private void validateDates(TransferRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();

        if (request.fechaEnvio().isBefore(now)) {
            throw new TransferValidationException("La fecha de envío no puede ser anterior a la fecha actual");
        }
        if (request.fechaLlegada().isBefore(now)) {
            throw new TransferValidationException("La fecha de llegada no puede ser anterior a la fecha actual");
        }
        if (request.fechaLlegada().isBefore(request.fechaEnvio())) {
            throw new TransferValidationException("La fecha de llegada no puede ser anterior a la fecha de envío");
        }
    }

    private void validateQuantity(TransferRequestDTO request) {
        if (request.cantidad() == null || request.cantidad() <= 0) {
            throw new TransferValidationException("La cantidad debe ser mayor a cero");
        }
    }

    private void validateStockAndQuantity(BigDecimal stock, Integer cantidad) {
        if (stock == null || stock.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferBusinessException("El producto tiene un stock inválido para realizar la transferencia");
        }
        if (BigDecimal.valueOf(cantidad.longValue()).compareTo(stock) > 0) {
            throw new TransferBusinessException("La cantidad a transferir no puede superar el stock disponible");
        }
    }

    private void applyInventoryMovement(Transfer transfer) {
        TransferProducts products = loadProductsFromTransfer(transfer);
        BigDecimal quantity = BigDecimal.valueOf(transfer.getCantidad().longValue());

        validateStockAndQuantity(products.originProduct().getStock(), transfer.getCantidad());

        products.originProduct().setStock(products.originProduct().getStock().subtract(quantity));
        products.destinationProduct().setStock(products.destinationProduct().getStock().add(quantity));

        productRepository.save(products.originProduct());
        productRepository.save(products.destinationProduct());
        transfer.setStock(products.originProduct().getStock());
    }

    private TransferProducts loadProductsFromTransfer(Transfer transfer) {
        Product originProduct = findProductByNameAndLocation(transfer.getProducto(), transfer.getSedeOrigen(), ORIGIN_ROLE);
        Product destinationProduct = findProductByNameAndLocation(transfer.getProducto(), transfer.getSedeDestino(), DESTINATION_ROLE);
        return new TransferProducts(originProduct, destinationProduct);
    }

    private Product findProductByNameAndLocation(String productName, String locationId, String role) {
        UUID locationUuid;
        try {
            locationUuid = UUID.fromString(locationId);
        } catch (IllegalArgumentException ex) {
            throw new TransferValidationException("La sede de " + role + " debe ser un UUID válido");
        }

        return productRepository.findAll().stream()
                .filter(product -> locationUuid.equals(product.getLocationId()))
                .filter(product -> product.getName() != null)
                .filter(product -> product.getName().trim().equalsIgnoreCase(productName.trim()))
                .findFirst()
                .orElseThrow(() -> new TransferNotFoundException(
                        "No existe el producto '" + productName + "' en la sede de " + role
                ));
    }

    private void validateLocationExistsAndActive(String locationId, String role) {
        UUID parsedLocationId;
        try {
            parsedLocationId = UUID.fromString(locationId);
        } catch (IllegalArgumentException ex) {
            throw new TransferValidationException("La sede de " + role + " debe ser un UUID válido");
        }

        locationRepository.findById(parsedLocationId)
                .filter(com.co.eatupapi.domain.inventory.location.LocationDomain::isActive)
                .orElseThrow(() -> new TransferNotFoundException("La sede de " + role + " no existe o está inactiva"));
    }

    private Transfer getDestinationTransfer(Long id, String sedeDestino) {
        validateId(id);
        validateRequiredLocationId(sedeDestino, DESTINATION_REQUIRED_MESSAGE, DESTINATION_ROLE);
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(TRANSFER_NOT_FOUND_MESSAGE + id));
        syncTransitStatus(transfer);

        if (!sedeDestino.trim().equals(transfer.getSedeDestino())) {
            throw new TransferBusinessException("Solo la sede destino puede realizar esta acción");
        }

        return transfer;
    }

    private void validateReceivableStatus(Transfer transfer) {
        if (transfer.getEstado() != TransferStatus.EN_TRANSITO) {
            throw new TransferBusinessException("Solo se pueden gestionar traslados en estado EN_TRANSITO");
        }
    }

    private void validateCancellationAllowed(Transfer transfer, String sedeOrigen) {
        validateRequiredLocationId(sedeOrigen, "La sede origen es obligatoria para cancelar", ORIGIN_ROLE);
        if (!sedeOrigen.trim().equals(transfer.getSedeOrigen())) {
            throw new TransferBusinessException("Solo la sede origen puede cancelar el traslado");
        }
        validateAuthenticatedUserBelongsToLocation(transfer.getSedeOrigen(), ORIGIN_ROLE,
                "Solo la sede origen autenticada puede cancelar el traslado");
        if (transfer.getEstado() != TransferStatus.EN_PROCESO) {
            throw new TransferBusinessException("La sede origen solo puede cancelar traslados en estado EN_PROCESO");
        }
    }

    private void validateTransitAllowed(Transfer transfer, String sedeOrigen) {
        validateRequiredLocationId(sedeOrigen, "La sede origen es obligatoria para enviar el traslado", ORIGIN_ROLE);
        if (!sedeOrigen.trim().equals(transfer.getSedeOrigen())) {
            throw new TransferBusinessException("Solo la sede origen puede marcar el traslado en tránsito");
        }
        validateAuthenticatedUserBelongsToLocation(transfer.getSedeOrigen(), ORIGIN_ROLE,
                "Solo la sede origen autenticada puede marcar el traslado en tránsito");
        if (transfer.getEstado() != TransferStatus.EN_PROCESO) {
            throw new TransferBusinessException("Solo se pueden enviar a tránsito traslados en estado EN_PROCESO");
        }
    }

    private void validateManualStatusUpdate(TransferStatus nextStatus) {
        if (nextStatus == TransferStatus.EN_TRANSITO) {
            throw new TransferBusinessException("El estado EN_TRANSITO se actualiza automáticamente");
        }
        if (nextStatus == TransferStatus.COMPLETADO || nextStatus == TransferStatus.RECLAMADO) {
            throw new TransferBusinessException(
                    "La sede destino debe gestionar esta acción usando los endpoints de confirmación o reclamo"
            );
        }
    }

    private void validateRequiredLocationId(String locationId, String message, String role) {
        if (locationId == null || locationId.isBlank()) {
            throw new TransferValidationException(message);
        }
        validateLocationExistsAndActive(locationId, role);
    }

    private void validateAuthenticatedUserBelongsToLocation(String locationId,
                                                            String role,
                                                            String mismatchMessage) {
        validateRequiredLocationId(locationId, "La sede de " + role + " es obligatoria", role);
        String authenticatedEmail = getAuthenticatedUserEmail();
        UserDomain authenticatedUser = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new TransferBusinessException("No se encontró el usuario autenticado"));

        if (authenticatedUser.getLocationId() == null) {
            throw new TransferBusinessException("El usuario autenticado no tiene una sede asociada");
        }

        if (!authenticatedUser.getLocationId().toString().equals(locationId.trim())) {
            throw new TransferBusinessException(mismatchMessage);
        }
    }

    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new TransferBusinessException("No se pudo identificar el usuario autenticado");
        }
        return authentication.getName();
    }

    private Transfer syncTransitStatus(Transfer transfer) {
        if (transfer.getEstado() == TransferStatus.EN_PROCESO
                && transfer.getFechaEnvio() != null
                && !LocalDateTime.now().isBefore(transfer.getFechaEnvio())) {
            transfer.setEstado(TransferStatus.EN_TRANSITO);
            return transferRepository.save(transfer);
        }
        return transfer;
    }

    private void validateStatusTransition(TransferStatus currentStatus, TransferStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }

        boolean validTransition = switch (currentStatus) {
            case EN_PROCESO -> nextStatus == TransferStatus.CANCELADO || nextStatus == TransferStatus.EN_TRANSITO;
            case EN_TRANSITO -> nextStatus == TransferStatus.COMPLETADO || nextStatus == TransferStatus.RECLAMADO;
            case COMPLETADO, RECLAMADO, CANCELADO -> false;
        };

        if (!validTransition) {
            throw new TransferBusinessException(
                    "No se permite cambiar el estado de " + currentStatus + " a " + nextStatus
            );
        }
    }
}
