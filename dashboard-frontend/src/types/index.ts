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
