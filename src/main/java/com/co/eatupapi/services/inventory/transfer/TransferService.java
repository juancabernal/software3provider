package com.co.eatupapi.services.inventory.transfer;

import com.co.eatupapi.dto.inventory.transfer.TransferObservacionUpdateDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferRequestDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferResponseDTO;
import com.co.eatupapi.dto.inventory.transfer.TransferStatusUpdateDTO;

import java.util.List;

public interface TransferService {
    TransferResponseDTO create(TransferRequestDTO request);

    TransferResponseDTO updateStatus(Long id, String sedeOrigen, TransferStatusUpdateDTO statusUpdate);

    TransferResponseDTO findById(Long id);

    List<TransferResponseDTO> findAll();

    List<TransferResponseDTO> findAllInTransit();

    List<TransferResponseDTO> findAllCompleted();

    List<TransferResponseDTO> findAllCancelled();

    List<TransferResponseDTO> findAllClaimed();

    List<TransferResponseDTO> findIncoming(String sedeDestino);

    TransferResponseDTO confirmReceipt(Long id, String sedeDestino);

    TransferResponseDTO claimReceipt(Long id, String sedeDestino, TransferObservacionUpdateDTO observacionUpdate);
}
