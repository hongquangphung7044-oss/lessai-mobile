package com.lessai.data.model

data class Segment(
    val id: String,
    val index: Int,
    val originalText: String,
    val status: SegmentStatus = SegmentStatus.PENDING
)

enum class SegmentStatus {
    PENDING, GENERATING, REVIEWING, APPLIED, IGNORED
}

data class RewriteSuggestion(
    val segmentId: String,
    val rewrittenText: String,
    val isApplied: Boolean = false
)

data class Session(
    val id: String,
    val fileName: String,
    val segments: List<Segment> = emptyList(),
    val suggestions: List<RewriteSuggestion> = emptyList()
)