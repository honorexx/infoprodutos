export type RoleCode = "SUPER_ADMIN" | "INSTRUCTOR" | "STUDENT";

export interface AuthResponse {
  accessToken: string;
  userId: string;
  name: string;
  email: string;
  roles: RoleCode[];
}

export interface MeResponse {
  id: string;
  name: string;
  email: string;
  roles: RoleCode[];
  lastLoginAt: string | null;
  createdAt: string;
}

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  status: "ACTIVE" | "BLOCKED";
  roles: RoleCode[];
  createdAt: string;
  lastLoginAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export type CourseStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type ModuleStatus = "DRAFT" | "PUBLISHED";
export type LessonStatus = "DRAFT" | "PUBLISHED";
export type LessonAccessType = "FREE_PREVIEW" | "ENROLLED_ONLY";

export interface CourseSummary {
  id: string;
  title: string;
  slug: string;
  coverImageUrl: string | null;
  workloadHours: number | null;
  status: CourseStatus;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface CourseInstructorSummary {
  userId: string;
  name: string;
  primary: boolean;
}

export interface Course {
  id: string;
  title: string;
  slug: string;
  description: string | null;
  coverImageUrl: string | null;
  workloadHours: number | null;
  status: CourseStatus;
  minCompletionPercentage: number;
  minPassingScore: number;
  certificateEnabled: boolean;
  maxQuizAttempts: number | null;
  createdByUserId: string;
  createdByName: string;
  instructors: CourseInstructorSummary[];
  publishedAt: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Lesson {
  id: string;
  moduleId: string;
  title: string;
  description: string | null;
  orderIndex: number;
  durationSeconds: number | null;
  accessType: LessonAccessType;
  status: LessonStatus;
  currentVideoAssetId: string | null;
}

export type AiJobStatus =
  | "PENDING"
  | "TRANSCRIBING"
  | "TRANSCRIBED"
  | "GENERATING"
  | "AWAITING_REVIEW"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

export interface VideoAsset {
  id: string;
  lessonId: string | null;
  originalFilename: string | null;
  mimeType: string | null;
  sizeBytes: number | null;
  durationSeconds: number | null;
  uploadStatus: string;
  processingStatus: string;
  failureReason: string | null;
  createdAt: string;
}

export interface AiJob {
  id: string;
  courseId: string;
  moduleId: string;
  lessonId: string;
  videoAssetId: string | null;
  transcriptId: string | null;
  status: AiJobStatus;
  provider: string | null;
  model: string | null;
  requestedQuestionCount: number;
  language: string;
  idempotencyKey: string;
  attemptCount: number;
  errorMessage: string | null;
  usageMetadata: Record<string, unknown> | null;
  createdAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface AiReviewOption {
  id: string;
  text: string;
  correct: boolean;
  orderIndex: number;
}

export interface AiReview {
  reviewId: string;
  questionId: string;
  jobId: string;
  reviewStatus: string;
  statement: string;
  explanation: string | null;
  difficulty: string;
  topic: string | null;
  questionStatus: string;
  options: AiReviewOption[];
  evidence: Record<string, unknown>;
  rawAiPayload: Record<string, unknown>;
  reviewedAt: string | null;
}

export interface CourseModule {
  id: string;
  courseId: string;
  title: string;
  description: string | null;
  orderIndex: number;
  status: ModuleStatus;
  lessons: Lesson[];
}

export interface ApiErrorBody {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  timestamp: string;
  correlationId: string | null;
  errors: { field: string; message: string }[];
}
