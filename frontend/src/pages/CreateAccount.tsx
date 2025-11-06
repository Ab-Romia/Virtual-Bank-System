import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, PlusCircle, Loader2, Wallet } from 'lucide-react';
import toast from 'react-hot-toast';
import { accountService, handleApiError } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { formatCurrency } from '../utils/format';

export default function CreateAccount() {
  const navigate = useNavigate();
  const { userId } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    accountType: 'SAVINGS' as 'SAVINGS' | 'CHECKING',
    initialBalance: 0,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!userId) {
      toast.error('User not authenticated');
      navigate('/login');
      return;
    }

    if (formData.initialBalance < 0) {
      toast.error('Initial balance cannot be negative');
      return;
    }

    setLoading(true);

    try {
      const response = await accountService.createAccount({
        userId,
        accountType: formData.accountType,
        initialBalance: formData.initialBalance,
      });
      toast.success(response.message || 'Account created successfully!');
      navigate('/dashboard');
    } catch (error) {
      const apiError = handleApiError(error);
      toast.error(apiError.message || 'Failed to create account');
    } finally {
      setLoading(false);
    }
  };

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
            <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
              <Wallet className="w-8 h-8 text-green-600" />
            </div>
            <h1 className="text-3xl font-bold text-gray-900">Create New Account</h1>
            <p className="text-gray-600 mt-2">Open a new savings or checking account</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Account Type */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">Account Type</label>
              <div className="grid grid-cols-2 gap-4">
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, accountType: 'SAVINGS' })}
                  className={`p-4 border-2 rounded-lg transition-all ${
                    formData.accountType === 'SAVINGS'
                      ? 'border-primary-600 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="text-center">
                    <PlusCircle
                      className={`w-8 h-8 mx-auto mb-2 ${
                        formData.accountType === 'SAVINGS' ? 'text-primary-600' : 'text-gray-400'
                      }`}
                    />
                    <h3 className="font-semibold text-gray-900">Savings</h3>
                    <p className="text-sm text-gray-600 mt-1">For long-term savings</p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, accountType: 'CHECKING' })}
                  className={`p-4 border-2 rounded-lg transition-all ${
                    formData.accountType === 'CHECKING'
                      ? 'border-primary-600 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="text-center">
                    <Wallet
                      className={`w-8 h-8 mx-auto mb-2 ${
                        formData.accountType === 'CHECKING' ? 'text-primary-600' : 'text-gray-400'
                      }`}
                    />
                    <h3 className="font-semibold text-gray-900">Checking</h3>
                    <p className="text-sm text-gray-600 mt-1">For daily transactions</p>
                  </div>
                </button>
              </div>
            </div>

            {/* Initial Balance */}
            <div>
              <label htmlFor="initialBalance" className="block text-sm font-medium text-gray-700 mb-2">
                Initial Balance
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500">$</span>
                <input
                  id="initialBalance"
                  name="initialBalance"
                  type="number"
                  step="0.01"
                  min="0"
                  required
                  value={formData.initialBalance}
                  onChange={(e) =>
                    setFormData({ ...formData, initialBalance: parseFloat(e.target.value) || 0 })
                  }
                  className="input-field pl-8"
                  placeholder="0.00"
                />
              </div>
              {formData.initialBalance > 0 && (
                <p className="mt-2 text-sm text-gray-600">
                  You will deposit {formatCurrency(formData.initialBalance)} into your new account
                </p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full btn-primary flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  Creating Account...
                </>
              ) : (
                <>
                  <PlusCircle className="w-5 h-5" />
                  Create Account
                </>
              )}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}
