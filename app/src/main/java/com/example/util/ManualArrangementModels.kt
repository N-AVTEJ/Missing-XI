package com.example.util

data class TeamArrangement(
    val teams: List<GeneratedTeam>,
    val joker: String? = null,
    val jokerId: String? = null,
    val excludedPlayers: List<Player> = emptyList(),
    val signature: String = ArrangementSignatureUtil.generateSignature(teams, joker)
)

data class MatchConstraints(
    val allPlayers: List<Player>,
    val numTeams: Int,
    val playerConstraints: Map<String, PlayerMatchConstraint> = emptyMap(),
    val fixedGroups: List<LockedPlayerGroup> = emptyList(),
    val fairnessSettings: FairnessSettings = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS,
    val allowTemporaryImbalance: Boolean = false
)

data class ArrangementValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ManualEditFairnessSnapshot(
    val pairAnalysis: CandidatePairAnalysis = CandidatePairAnalysis(),
    val opponentAnalysis: OpponentPairAnalysis = OpponentPairAnalysis(),
    val strengthAnalysis: CandidateStrengthAnalysis = CandidateStrengthAnalysis(),
    val evaluation: FairnessEvaluation = FairnessEvaluation(),
    val qualityResult: FairnessQualityResult = FairnessQualityResult(),
    val fairnessScore: Int = 0,
    val fairnessRating: String = "",
    val isDuplicateArrangement: Boolean = false,
    val fairnessWarnings: List<String> = emptyList()
)

data class ManualEditOperationResult(
    val success: Boolean,
    val message: String? = null,
    val validationResult: ArrangementValidationResult? = null
)

data class ManualEditState(
    val isEditing: Boolean = true,
    val originalArrangement: TeamArrangement,
    val currentArrangement: TeamArrangement,
    val originalFairness: ManualEditFairnessSnapshot,
    val currentFairness: ManualEditFairnessSnapshot,
    val validationResult: ArrangementValidationResult,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoCount: Int = 0,
    val redoCount: Int = 0,
    val allowTemporaryImbalance: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val selectedPlayerId: String? = null,
    val lastOperationError: String? = null,
    val lastOperationInfo: String? = null,
    val isDuplicateArrangement: Boolean = false,
    val playerMap: Map<String, Player> = emptyMap(),
    val constraints: Map<String, PlayerMatchConstraint> = emptyMap(),
    val fixedGroups: List<LockedPlayerGroup> = emptyList()
)

data class ConfirmedManualArrangement(
    val finalArrangement: TeamArrangement,
    val originalArrangement: TeamArrangement,
    val finalFairness: ManualEditFairnessSnapshot,
    val validationResult: ArrangementValidationResult,
    val wasModified: Boolean,
    val isDuplicateArrangement: Boolean
)
