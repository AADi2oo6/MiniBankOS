package com.minibankos;

import Auth.AuthManager;
import kernel.ModeBit;
import kernel.RecoveryManager;
import kernel.TransactionManager;
import kernel.scheduler.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import system.BankDatabase;

@Configuration
public class MiniBankConfig {

    @Bean
    public BankDatabase bankDatabase() {
        return new BankDatabase();
    }

    @Bean
    public Scheduler scheduler() {
        return new Scheduler();
    }

    @Bean
    public ModeBit modeBit() {
        return new ModeBit();
    }

    @Bean
    public AuthManager authManager() {
        return new AuthManager();
    }

    @Bean
    public TransactionManager transactionManager(
            BankDatabase bank, 
            Scheduler scheduler, 
            AuthManager authManager, 
            ModeBit modeBit) {
        
        TransactionManager tm = new TransactionManager(bank, scheduler, authManager, modeBit);
        // Start recovery inside bean initialization
        RecoveryManager recovery = new RecoveryManager(tm);
        recovery.recover();
        
        return tm;
    }
}
