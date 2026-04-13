package com.mutuelle.service;

import com.mutuelle.entity.Agape;
import com.mutuelle.entity.Administrator;
import com.mutuelle.entity.Cashbox;
import com.mutuelle.entity.Session;
import com.mutuelle.enums.CashboxName;
import com.mutuelle.exception.BusinessException;
import com.mutuelle.repository.AgapeRepository;
import com.mutuelle.repository.CashboxRepository;
import com.mutuelle.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgapeService {

    private final AgapeRepository agapeRepository;
    private final CashboxRepository cashboxRepository;
    private final SessionRepository sessionRepository;
    private final TransactionService transactionService;

    @Transactional
    public Agape createAgape(String title, String description, BigDecimal amount, LocalDate date, Long sessionId, Administrator admin) {
        // Enforce financing from Solidarity Fund (Fonds Social)
        Cashbox solidarityBox = cashboxRepository.findByName(CashboxName.SOLIDARITY)
                .orElseThrow(() -> new BusinessException("Caisse de solidarité introuvable"));

        if (solidarityBox.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Fonds insuffisants dans le Fonds Social pour cette Agape");
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Session introuvable"));

        Agape agape = Agape.builder()
                .title(title)
                .description(description)
                .amount(amount)
                .eventDate(date)
                .session(session)
                .createdBy(admin)
                .build();

        Agape savedAgape = agapeRepository.save(agape);

        // Deduct from Solidarity Cashbox
        transactionService.recordTransaction(
                amount.negate(),
                "AGAPE",
                "Agape: " + title,
                solidarityBox,
                null // Not tied to a specific member
        );

        return savedAgape;
    }

    public List<Agape> getAllAgapes() {
        return agapeRepository.findAll();
    }
}
