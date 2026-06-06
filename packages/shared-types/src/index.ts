// =============================================
// 智能备忘录 — 共享类型定义
// 所有前端应用（Web、Desktop、Mobile）共用
// =============================================

/**
 * 统一 API 响应格式
 */
export interface ApiResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

/**
 * 游标分页响应
 */
export interface PaginatedResponse<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
}

/**
 * 错误码枚举
 */
export const ErrorCode = {
  // 认证鉴权
  AUTH_TOKEN_EXPIRED: 'AUTH_TOKEN_EXPIRED',
  AUTH_TOKEN_INVALID: 'AUTH_TOKEN_INVALID',
  AUTH_UNAUTHORIZED: 'AUTH_UNAUTHORIZED',

  // 用户
  USER_NOT_FOUND: 'USER_NOT_FOUND',
  USER_ALREADY_EXISTS: 'USER_ALREADY_EXISTS',
  USER_DISABLED: 'USER_DISABLED',

  // 备忘录
  MEMO_NOT_FOUND: 'MEMO_NOT_FOUND',
  MEMO_VERSION_CONFLICT: 'MEMO_VERSION_CONFLICT',

  // 同步
  SYNC_CONFLICT: 'SYNC_CONFLICT',
  SYNC_CURSOR_INVALID: 'SYNC_CURSOR_INVALID',

  // 语音识别
  ASR_TASK_FAILED: 'ASR_TASK_FAILED',
  ASR_UNSUPPORTED_FORMAT: 'ASR_UNSUPPORTED_FORMAT',

  // AI 语义
  AI_TIMEOUT: 'AI_TIMEOUT',
  AI_MODEL_ERROR: 'AI_MODEL_ERROR',

  // 蓝牙
  BLE_DEVICE_NOT_FOUND: 'BLE_DEVICE_NOT_FOUND',
  BLE_BINDING_FAILED: 'BLE_BINDING_FAILED',

  // 推送
  PUSH_TOKEN_INVALID: 'PUSH_TOKEN_INVALID',
  PUSH_SEND_FAILED: 'PUSH_SEND_FAILED',

  // 文件
  FILE_NOT_FOUND: 'FILE_NOT_FOUND',
  FILE_TOO_LARGE: 'FILE_TOO_LARGE',

  // 系统
  SYS_ERROR: 'SYS_ERROR',
  SYS_RATE_LIMITED: 'SYS_RATE_LIMITED',
  SYS_SERVICE_UNAVAILABLE: 'SYS_SERVICE_UNAVAILABLE',
} as const;

export type ErrorCodeType = (typeof ErrorCode)[keyof typeof ErrorCode];

// =============================================
// 认证相关类型
// =============================================

/** 注册请求 */
export interface RegisterRequest {
  username: string;
  password: string;
  email?: string;
  phone?: string;
}

/** 登录请求 */
export interface LoginRequest {
  username: string;
  password: string;
  deviceType?: 'ios' | 'android' | 'web' | 'desktop';
  deviceName?: string;
}

/** 刷新 Token 请求 */
export interface RefreshRequest {
  refreshToken: string;
}

/** 认证响应 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // Access Token 有效期（秒）
}

/** 用户信息 */
export interface UserInfo {
  id: string;
  username: string;
  email: string | null;
  phone: string | null;
  status: string;
  createdAt: string;
}

/** 用户设备 */
export interface UserDevice {
  id: string;
  deviceType: string;
  deviceName: string | null;
  lastOnlineAt: string | null;
  createdAt: string;
}

/** 更新用户请求 */
export interface UpdateUserRequest {
  email?: string;
  phone?: string;
}

// =============================================
// 备忘录相关类型
// =============================================

export interface CreateMemoRequest {
  title: string;
  content?: string;
  categoryId?: string;
  tagIds?: string[];
  isPinned?: boolean;
}

export interface UpdateMemoRequest {
  title?: string;
  content?: string;
  categoryId?: string;
  tagIds?: string[];
  isPinned?: boolean;
}

export interface MemoResponse {
  id: string;
  title: string;
  content: string | null;
  categoryId: string | null;
  status: 'active' | 'archived' | 'deleted';
  isPinned: boolean;
  tagIds: string[];
  createdAt: string;
  updatedAt: string;
}

export interface MemoListResponse {
  items: MemoResponse[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface CreateTagRequest {
  name: string;
  color?: string;
}

export interface TagResponse {
  id: string;
  name: string;
  color: string | null;
}

export interface CreateCategoryRequest {
  name: string;
  color?: string;
  sortOrder?: number;
}

export interface CategoryResponse {
  id: string;
  name: string;
  color: string | null;
  sortOrder: number;
}

export interface AttachmentResponse {
  id: string;
  memoId: string;
  fileName: string;
  fileSize: number;
  contentType: string | null;
  createdAt: string;
}

/** 绑定设备请求 */
export interface BindDeviceRequest {
  deviceType: 'ios' | 'android' | 'web' | 'desktop';
  deviceName?: string;
  pushToken?: string;
  pushProvider?: 'apns' | 'fcm' | 'huawei' | 'xiaomi';
}

/**
 * 服务名称
 */
export type ServiceName =
  | 'api-gateway'
  | 'user-service'
  | 'memo-service'
  | 'sync-service'
  | 'asr-service'
  | 'ai-service'
  | 'bluetooth-service'
  | 'push-service'
  | 'file-service'
  | 'search-service';
