/*
 * Copyright (c) 2020, Belieal <https://github.com/Belieal>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flippingutilities.controller;

import com.flippingutilities.db.TradePersister;
import com.flippingutilities.model.AccountData;
import com.flippingutilities.model.AccountWideData;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * Responsible for loading data from disk, handling any operations to access/change data during the plugin's life, and storing
 * data to disk.
 */
@Slf4j
public class DataHandler {
    FlippingPlugin plugin;
    private AccountWideData accountWideData;
    private Map<String, AccountData> accountSpecificData = new HashMap<>();
    private boolean accountWideDataChanged = false;
    private Set<String> accountsWithUnsavedChanges = new HashSet<>();
    public String thisClientLastStored;

    public DataHandler(FlippingPlugin plugin) {
        this.plugin = plugin;
    }

    public AccountWideData viewAccountWideData() {
        return accountWideData;
    }

    public AccountWideData getAccountWideData() {
        accountWideDataChanged = true;
        return accountWideData;
    }

    public void addAccount(String displayName) {
        log.info("adding {} to data handler", displayName);
        AccountData accountData = new AccountData();
        accountData.prepareForUse(plugin);
        accountSpecificData.put(displayName, accountData);
    }

    public void deleteAccount(String displayName) {
        log.info("deleting account: {}", displayName);
        accountSpecificData.remove(displayName);
        TradePersister.deleteFile(displayName + ".json");
    }

    public Collection<AccountData> getAllAccountData() {
        accountsWithUnsavedChanges.addAll(accountSpecificData.keySet());
        return accountSpecificData.values();
    }

    public Collection<AccountData> viewAllAccountData() {
        return accountSpecificData.values();
    }

    //calls it if data is going to be updated,
    public AccountData getAccountData(String displayName) {
        accountsWithUnsavedChanges.add(displayName);
        return accountSpecificData.get(displayName);
    }

    //is called if account data just needs to be viewed, not updated
    public AccountData viewAccountData(String displayName) {
        return accountSpecificData.get(displayName);
    }

    public Set<String> currentAccounts() {
        return accountSpecificData.keySet();
    }

    public void markDataAsHavingChanged(String displayName) {
        if (displayName.equals(FlippingPlugin.ACCOUNT_WIDE)) {
            accountWideDataChanged = true;
        }
        else {
            accountsWithUnsavedChanges.add(displayName);
        }
    }

    public void storeData() {
        log.info("storing data");
        if (accountsWithUnsavedChanges.size() > 0) {
            log.info("accounts with unsaved changes are {}. Saving them.", accountsWithUnsavedChanges);
            accountsWithUnsavedChanges.forEach(accountName -> storeAccountData(accountName));
            accountsWithUnsavedChanges.clear();
        }

        if (accountWideDataChanged) {
            log.info("account wide data changed, saving it.");
            storeAccountWideData();
            accountWideDataChanged = false;
        }
    }

    public void loadData() {
        try {
            log.info("initiating load");
            TradePersister.setup();
            accountWideData = fetchAccountWideData();
            accountSpecificData = fetchAllAccountData();
        }
        catch (IOException e) {
            log.info("error while loading data, setting accountwidedata and accountspecific to defaults", e);
            accountWideData = new AccountWideData();
            accountWideData.prepare();
            accountSpecificData = new HashMap<>();
            accountWideDataChanged = true;
        }
    }

    public void loadAccountWideData() {
        log.info("updating account wide data");
        accountWideData = fetchAccountWideData();
    }

    public void loadAccountData(String displayName) {
        log.info("loading data for {}", displayName);
        accountSpecificData.put(displayName, fetchAccountData(displayName));
    }

    private AccountWideData fetchAccountWideData() {
        try {
            log.info("loading account wide data");
            AccountWideData accountWideData = TradePersister.loadAccountWideData();
            if (accountWideData.getOptions().isEmpty()) {
                accountWideData.prepare();
                accountWideDataChanged = true;
            }
            log.info("successfully loaded account wide data");
            return accountWideData;
        }
        catch (IOException e) {
            log.info("couldn't load accountwide data", e);
            AccountWideData accountWideData = new AccountWideData();
            accountWideData.prepare();
            accountWideDataChanged = true;
            return accountWideData;
        }
    }

    private Map<String, AccountData> fetchAllAccountData()
    {
        try
        {
            Map<String, AccountData> trades = TradePersister.loadAllAccounts();
            trades.values().forEach(accountData -> {
                accountData.startNewSession();
                accountData.prepareForUse(plugin);
            });
            log.info("successfully loaded trades");
            return trades;
        }
        catch (IOException e)
        {
            log.info("couldn't load trades, error: " + e);
            return new HashMap<>();
        }
    }

    private AccountData fetchAccountData(String displayName)
    {
        try
        {
            AccountData accountData = TradePersister.loadAccount(displayName);
            accountData.prepareForUse(plugin);
            return accountData;
        }
        catch (IOException e)
        {
            log.info("couldn't load trades for {}, e = " + e, displayName);
            return new AccountData();
        }
    }

    private void storeAccountData(String displayName)
    {
        try
        {
            AccountData data = accountSpecificData.get(displayName);
            if (data == null)
            {
                log.info("for an unknown reason the data associated with {} has been set to null. Storing" +
                        "an empty AccountData object instead.", displayName);
                data = new AccountData();
            }
            thisClientLastStored = displayName;
            TradePersister.storeTrades(displayName, data);
            log.info("successfully stored trades for {}", displayName);
        }
        catch (IOException e)
        {
            log.info("couldn't store trades, error = " + e);
        }
    }

    private void storeAccountWideData() {
        try {
            TradePersister.storeTrades("accountwide", accountWideData);
            log.info("successfully stored account wide data");
        }
        catch (IOException e) {
            log.info("couldn't store trades", e);
        }
    }




}
