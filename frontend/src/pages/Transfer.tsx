import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Send, Loader2, ArrowRightLeft } from 'lucide-react';
import toast from 'react-hot-toast';
import { accountService, transactionService, handleApiError } from '../services/api';
import { useAuthStore } from '../store/authStore';
import type { UserAccountsResponse } from '../types/api';
import { formatCurrency } from '../utils/format';

export default function Transfer() {
  const navigate = useNavigate();
  const { userId } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [accountsLoading, setAccountsLoading] = useState(true);
  const [accounts, setAccounts] = useState<UserAccountsResponse[]>([]);
  const [formData, setFormData] = useState({
    fromAccountId: '',
    toAccountId: '',
    amount: 0,
    description: '',
  });

  useEffect(() => {
    const fetchAccounts = async () => {
      if (!userId) {
        navigate('/login');
        return;
      }

      try {
        const data = await accountService.getUserAccounts(userId);
        setAccounts(data);
        if (data.length > 0) {
          setFormData((prev) => ({ ...prev, fromAccountId: data[0].accountId }));
        }
      } catch (error) {
        const apiError = handleApiError(error);
        toast.error(apiError.message || 'Failed to load accounts');
      } finally {
        setAccountsLoading(false);
      }
    };

    fetchAccounts();
  }, [userId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.fromAccountId || !formData.toAccountId) {
      toast.error('Please select both accounts');
      return;
    }

    if (formData.fromAccountId === formData.toAccountId) {
      toast.error('Cannot transfer to the same account');
      return;
    }

    if (formData.amount <= 0) {
      toast.error('Amount must be greater than 0');
      return;
    }

    setLoading(true);

    try {
      // Step 1: Initiate transfer
      const initiationResponse = await transactionService.initiateTransfer({
        fromAccountId: formData.fromAccountId,
        toAccountId: formData.toAccountId,
        amount: formData.amount,
        description: formData.description,
      });

      toast.success('Transfer initiated!');

      // Step 2: Execute transfer
      const executionResponse = await transactionService.executeTransfer({
        transactionId: initiationResponse.transactionId,
      });

      if (executionResponse.status === 'SUCCESS') {
        toast.success('Transfer completed successfully!');
        navigate('/dashboard');
      } else {
        toast.error('Transfer failed');
      }
    } catch (error) {
      const apiError = handleApiError(error);
      toast.error(apiError.message || 'Transfer failed');
    } finally {
      setLoading(false);
    }
  };

  const fromAccount = accounts.find((acc) => acc.accountId === formData.fromAccountId);

  if (accountsLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <Loader2 className="w-12 h-12 text-primary-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading accounts...</p>
        </div>
      </div>
    );
  }

  if (accounts.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50">
        <header className="bg-white shadow-sm border-b border-gray-200">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
            <button
              onClick={() => navigate('/dashboard')}
              className="flex items-center gap-2 text-gray-600 hover:text-gray-900"
            >
              <ArrowLeft className="w-5 h-5" />
              Back to Dashboard
            </button>
          </div>
        </header>
        <main className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="card text-center py-12">
            <ArrowRightLeft className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-600 mb-4">You need at least one account to make transfers</p>
            <button onClick={() => navigate('/create-account')} className="btn-primary">
              Create Account
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <button
            onClick={() => navigate('/dashboard')}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900"
          >
            <ArrowLeft className="w-5 h-5" />
            Back to Dashboard
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="card">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-4">
              <ArrowRightLeft className="w-8 h-8 text-blue-600" />
            </div>
            <h1 className="text-3xl font-bold text-gray-900">Transfer Money</h1>
            <p className="text-gray-600 mt-2">Send money to another account</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* From Account */}
            <div>
              <label htmlFor="fromAccountId" className="block text-sm font-medium text-gray-700 mb-2">
                From Account
              </label>
              <select
                id="fromAccountId"
                name="fromAccountId"
                required
                value={formData.fromAccountId}
                onChange={(e) => setFormData({ ...formData, fromAccountId: e.target.value })}
                className="input-field"
              >
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.accountType} - {account.accountNumber} ({formatCurrency(account.balance)})
                  </option>
                ))}
              </select>
              {fromAccount && (
                <p className="mt-2 text-sm text-gray-600">
                  Available balance: {formatCurrency(fromAccount.balance)}
                </p>
              )}
            </div>

            {/* To Account ID */}
            <div>
              <label htmlFor="toAccountId" className="block text-sm font-medium text-gray-700 mb-2">
                To Account ID
              </label>
              <input
                id="toAccountId"
                name="toAccountId"
                type="text"
                required
                value={formData.toAccountId}
                onChange={(e) => setFormData({ ...formData, toAccountId: e.target.value })}
                className="input-field"
                placeholder="Enter recipient account ID (UUID)"
              />
              <p className="mt-2 text-sm text-gray-500">
                Enter the UUID of the account you want to send money to
              </p>
            </div>

            {/* Amount */}
            <div>
              <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-2">
                Amount
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500">$</span>
                <input
                  id="amount"
                  name="amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={formData.amount}
                  onChange={(e) =>
                    setFormData({ ...formData, amount: parseFloat(e.target.value) || 0 })
                  }
                  className="input-field pl-8"
                  placeholder="0.00"
                />
              </div>
              {formData.amount > 0 && fromAccount && formData.amount > fromAccount.balance && (
                <p className="mt-2 text-sm text-red-600">Insufficient funds</p>
              )}
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
                Description (Optional)
              </label>
              <input
                id="description"
                name="description"
                type="text"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="input-field"
                placeholder="e.g., Rent payment, Gift, etc."
              />
            </div>

            {/* Transfer Summary */}
            {formData.amount > 0 && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 className="font-medium text-blue-900 mb-2">Transfer Summary</h3>
                <div className="space-y-1 text-sm text-blue-800">
                  <p>Amount: {formatCurrency(formData.amount)}</p>
                  {formData.description && <p>Description: {formData.description}</p>}
                </div>
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={
                loading ||
                (fromAccount && formData.amount > fromAccount.balance) ||
                formData.amount <= 0
              }
              className="w-full btn-primary flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  Processing Transfer...
                </>
              ) : (
                <>
                  <Send className="w-5 h-5" />
                  Transfer {formData.amount > 0 && formatCurrency(formData.amount)}
                </>
              )}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}
