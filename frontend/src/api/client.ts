import axios, { AxiosError } from 'axios';
import {
  ApiResponse,
  AuthResponse,
  CreateTransactionRequest,
  EvidenceRecordDto,
  InferredStateDto,
  IngestEventRequest,
  PagedResponse,
  RecoveryAnalysisDto,
  ReplayExecutionDto,
  ReplayScenarioType,
  StateTransitionDto,
  SystemStatusDto,
  TransactionDto,
  TransactionEventDto,
  UserDto
} from '../types';

export interface CreateTxPayload {
  externalId: string;
  title: string;
  channel?: string;
  amount?: number;
  currency?: string;
  metadataJson?: string;
}

export interface ReplayPayload {
  scenarioType: ReplayScenarioType;
  targetEventId?: string;
  delayMillis?: number;
  conflictingPayload?: string;
}

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('statesnap_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('statesnap_token');
      localStorage.removeItem('statesnap_user');
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    return Promise.reject(error);
  }
);

export const apiClient = {
  // System Health
  system: {
    status: async (): Promise<SystemStatusDto> => {
      const res = await api.get<ApiResponse<SystemStatusDto>>('/system/status');
      return res.data.data;
    },
  },

  // Authentication
  auth: {
    login: async (email: string, password: string): Promise<AuthResponse> => {
      const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
      return res.data.data;
    },
    register: async (email: string, password: string, fullName: string): Promise<AuthResponse> => {
      const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', { email, password, fullName });
      return res.data.data;
    },
    me: async (): Promise<UserDto> => {
      const res = await api.get<ApiResponse<UserDto>>('/auth/me');
      return res.data.data;
    },
  },

  // Transactions
  transactions: {
    list: async (
      status?: string,
      search?: string,
      page: number = 0,
      size: number = 15
    ): Promise<PagedResponse<TransactionDto>> => {
      const params: Record<string, string | number> = { page, size };
      if (status && status !== 'ALL') params.status = status;
      if (search) params.search = search;
      const res = await api.get<ApiResponse<PagedResponse<TransactionDto>>>('/transactions', { params });
      return res.data.data;
    },
    get: async (id: string): Promise<TransactionDto> => {
      const res = await api.get<ApiResponse<TransactionDto>>(`/transactions/${id}`);
      return res.data.data;
    },
    create: async (payload: CreateTxPayload): Promise<TransactionDto> => {
      const res = await api.post<ApiResponse<TransactionDto>>('/transactions', payload);
      return res.data.data;
    },
  },

  // Events
  events: {
    list: async (txId: string): Promise<TransactionEventDto[]> => {
      const res = await api.get<ApiResponse<TransactionEventDto[]>>(`/transactions/${txId}/events`);
      return res.data.data;
    },
    ingest: async (txId: string, event: IngestEventRequest): Promise<TransactionEventDto> => {
      const res = await api.post<ApiResponse<TransactionEventDto>>(`/transactions/${txId}/events`, event);
      return res.data.data;
    },
    batchIngest: async (txId: string, events: IngestEventRequest[]): Promise<TransactionEventDto[]> => {
      const res = await api.post<ApiResponse<TransactionEventDto[]>>(`/transactions/${txId}/events/batch`, { events });
      return res.data.data;
    },
  },

  // Reconstruction & Evidence
  reconstruction: {
    reconstruct: async (txId: string): Promise<InferredStateDto> => {
      const res = await api.post<ApiResponse<InferredStateDto>>(`/transactions/${txId}/reconstruct`);
      return res.data.data;
    },
    getLatest: async (txId: string): Promise<InferredStateDto> => {
      const res = await api.get<ApiResponse<InferredStateDto>>(`/transactions/${txId}/reconstruction`);
      return res.data.data;
    },
    getEvidence: async (txId: string): Promise<EvidenceRecordDto[]> => {
      const res = await api.get<ApiResponse<EvidenceRecordDto[]>>(`/transactions/${txId}/evidence`);
      return res.data.data;
    },
    getTransitions: async (txId: string): Promise<StateTransitionDto[]> => {
      const res = await api.get<ApiResponse<StateTransitionDto[]>>(`/transactions/${txId}/transitions`);
      return res.data.data;
    },
  },

  // Replay Lab
  replay: {
    execute: async (txId: string, payload: ReplayPayload): Promise<ReplayExecutionDto> => {
      const res = await api.post<ApiResponse<ReplayExecutionDto>>(`/transactions/${txId}/replay`, payload);
      return res.data.data;
    },
    list: async (txId: string): Promise<ReplayExecutionDto[]> => {
      const res = await api.get<ApiResponse<ReplayExecutionDto[]>>(`/transactions/${txId}/replays`);
      return res.data.data;
    },
    get: async (executionId: string): Promise<ReplayExecutionDto> => {
      const res = await api.get<ApiResponse<ReplayExecutionDto>>(`/replay/${executionId}`);
      return res.data.data;
    },
  },

  // Recovery Advisor
  recovery: {
    get: async (txId: string): Promise<RecoveryAnalysisDto> => {
      const res = await api.get<ApiResponse<RecoveryAnalysisDto>>(`/transactions/${txId}/recovery`);
      return res.data.data;
    },
    analyze: async (txId: string): Promise<RecoveryAnalysisDto> => {
      const res = await api.post<ApiResponse<RecoveryAnalysisDto>>(`/transactions/${txId}/recovery/analyze`);
      return res.data.data;
    },
  },
};

