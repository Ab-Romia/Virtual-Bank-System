import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { accounts, transfers, users } from './endpoints';
import type { CreateAccountRequest } from './types';

export const queryKeys = {
  profile: ['profile'] as const,
  accounts: ['accounts'] as const,
  transfers: ['transfers'] as const,
  transfer: (id: string) => ['transfer', id] as const,
  audit: (id: string) => ['audit', id] as const,
};

export function useProfile() {
  return useQuery({ queryKey: queryKeys.profile, queryFn: users.me });
}

export function useAccounts() {
  return useQuery({ queryKey: queryKeys.accounts, queryFn: accounts.list });
}

export function useTransfers() {
  return useQuery({ queryKey: queryKeys.transfers, queryFn: transfers.list });
}

export function useCreateAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateAccountRequest) => accounts.create(body),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.accounts }),
  });
}

export function useDeposit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, amount }: { id: string; amount: string }) =>
      accounts.deposit(id, amount),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.accounts }),
  });
}
