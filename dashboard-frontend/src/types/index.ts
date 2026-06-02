export interface ReviewRecord {
  id: string
  repoFullName: string
  prNumber: number
  prTitle: string
  prAuthor: string
  headSha: string
  status: ReviewStatus
  summary: string | null
  comments: ReviewComment[]
  sandboxExecutions: SandboxExecution[]
  createdAt: string
  completedAt: string | null
}

export interface ReviewComment {
  id: number
  filePath: string
  lineNumber: number
  severity: string
  message: string
  suggestion: string | null
}

export interface SandboxExecution {
  id: number
  filePath: string
  className: string
  executionStatus: ExecutionStatus
  exitCode: number
  stdout: string | null
  stderr: string | null
  timedOut: boolean
  compilationFailed: boolean
  executionTimeMs: number
  executedAt: string
}

export type ExecutionStatus =
  | 'SUCCESS'
  | 'COMPILATION_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIMEOUT'
  | 'SANDBOX_ERROR'

export interface ProgressMessage {
  reviewId: string
  repoFullName: string
  prNumber: number
  status: ReviewStatus
  details: string
  timestamp: string
}

export type ReviewStatus =
  | 'QUEUED'
  | 'CLONING_REPO'
  | 'FETCHING_DIFF'
  | 'ANALYZING'
  | 'SANDBOXING'
  | 'POSTING'
  | 'COMPLETED'
  | 'FAILED'
