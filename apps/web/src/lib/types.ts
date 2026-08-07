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

export type EnrollmentStatus = "ACTIVE" | "SUSPENDED" | "CANCELLED" | "EXPIRED";
export type LessonProgressStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";

export interface Enrollment {
  id: string;
  studentUserId: string;
  studentName: string;
  studentEmail: string;
  courseId: string;
  courseTitle: string;
  status: EnrollmentStatus;
  startedAt: string;
  expiresAt: string | null;
  grantedByUserId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LessonProgressItem {
  lessonId: string;
  title: string;
  orderIndex: number;
  durationSeconds: number | null;
  accessType: LessonAccessType;
  progressStatus: LessonProgressStatus;
  lastPositionSeconds: number;
  currentVideoAssetId: string | null;
}

export interface ModuleProgressSummary {
  moduleId: string;
  moduleTitle: string;
  orderIndex: number;
  totalPublishedLessons: number;
  completedLessons: number;
  completionPercent: number;
  lessons: LessonProgressItem[];
}

export interface ProgressSummary {
  enrollmentId: string;
  courseId: string;
  courseTitle: string;
  enrollmentStatus: EnrollmentStatus;
  totalPublishedLessons: number;
  completedLessons: number;
  courseCompletionPercent: number;
  canFinishCourse: boolean;
  courseCompletedAt: string | null;
  canIssueCertificate: boolean;
  certificateId: string | null;
  modules: ModuleProgressSummary[];
}

export interface StreamUrl {
  url: string;
  expiresAt: number;
  ttlSeconds: number;
}

export interface LessonProgress {
  id: string;
  enrollmentId: string;
  lessonId: string;
  status: LessonProgressStatus;
  lastPositionSeconds: number;
  startedAt: string | null;
  completedAt: string | null;
  updatedAt: string;
}

export interface Certificate {
  id: string;
  enrollmentId: string;
  courseId: string;
  studentName: string;
  courseTitle: string;
  workloadHours: number;
  completionDate: string;
  issuedAt: string;
  validationCode: string;
  validationUrl: string;
  status: string;
  coordinatorName: string;
  chiefVisionOfficerName: string;
}

export interface PublicCertificateValidation {
  valid: boolean;
  status: string;
  studentName: string;
  courseTitle: string;
  workloadHours: number;
  completionDate: string;
  validationCode: string;
  issuedDate: string;
}

export interface QuizDetail {
  id: string | null;
  moduleId: string;
  title: string;
  status: string;
  passingScore: number | null;
  maxAttempts: number | null;
  publishedQuestionCount: number;
  questions: QuestionStaff[];
}

export interface QuestionStaff {
  id: string;
  quizId: string;
  lessonId: string;
  statement: string;
  explanation: string | null;
  difficulty: string;
  topic: string | null;
  status: string;
  origin: string;
  orderIndex: number;
  createdAt: string;
  options: { id: string; text: string; correct: boolean; orderIndex: number }[];
}

export interface QuizTake {
  quizId: string;
  moduleId: string;
  title: string;
  maxAttempts: number | null;
  attemptsUsed: number;
  canStartNewAttempt: boolean;
  inProgressAttemptId: string | null;
  questions: {
    id: string;
    statement: string;
    orderIndex: number;
    options: { id: string; text: string; orderIndex: number }[];
  }[];
}

export interface QuizAttempt {
  id: string;
  enrollmentId: string;
  quizId: string;
  attemptNumber: number;
  status: string;
  startedAt: string;
  submittedAt: string | null;
  score: number | null;
  passed: boolean | null;
  answers: {
    questionId: string;
    statement: string;
    selectedOptionId: string | null;
    selectedOptionText: string | null;
    correct: boolean;
    correctOptionId: string | null;
    explanation: string | null;
  }[];
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
