// src/store/DataStore.js
const KEYS = {
  WALLETS: "wallets",
  CATEGORIES_EXPENSE: "categories_expense",
  CATEGORIES_INCOME: "categories_income",
  TRANSACTIONS: "transactions",
};

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

function write(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}

export const DataStore = {
  keys: KEYS,

  getWallets() { return read(KEYS.WALLETS, []); },
  setWallets(list) { write(KEYS.WALLETS, list); },

  getExpenseCategories() { return read(KEYS.CATEGORIES_EXPENSE, []); },
  setExpenseCategories(list) { write(KEYS.CATEGORIES_EXPENSE, list); },

  getIncomeCategories() { return read(KEYS.CATEGORIES_INCOME, []); },
  setIncomeCategories(list) { write(KEYS.CATEGORIES_INCOME, list); },

  getTransactions() { return read(KEYS.TRANSACTIONS, []); },
  setTransactions(list) { write(KEYS.TRANSACTIONS, list); },
};
