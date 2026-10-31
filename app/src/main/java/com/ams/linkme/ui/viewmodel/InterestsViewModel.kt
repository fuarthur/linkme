package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ams.linkme.domain.InterestCatalog

data class InterestEditorState(
    val selected: List<String>,
    val query: String,
    val groupId: String?,
    val choices: Set<String>,
    val step: Int,
    val visibleTopics: Int,
    val error: String? = null,
)

/** A local draft. Only the caller decides whether to publish or use it as a filter. */
class InterestsViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val introduction: String = saved.get<String>("introduction").orEmpty()
    val purpose: String = saved.get<String>("editorPurpose") ?: "profile"
    val state = MutableLiveData(snapshot())

    private fun snapshot(error: String? = null) =
        InterestEditorState(
            InterestCatalog.normalize(saved.get<ArrayList<String>>("selectedInterests").orEmpty()),
            saved.get<String>("query").orEmpty(),
            saved["groupId"],
            saved.get<ArrayList<String>>("choices").orEmpty().toSet(),
            saved.get<Int>("step") ?: -1,
            saved.get<Int>("visibleTopics") ?: 18,
            error,
        )

    private fun emit(error: String? = null) {
        state.value = snapshot(error)
    }

    private fun same(a: String, b: String) = InterestCatalog.normalize(listOf(a, b)).size == 1

    fun query(value: String) {
        if (value == state.value!!.query) return
        saved["query"] = value
        emit()
    }

    fun group(id: String?) {
        saved["groupId"] = id
        saved["visibleTopics"] = 18
        emit()
    }

    fun more() {
        saved["visibleTopics"] = state.value!!.visibleTopics + 18
        emit()
    }

    fun add(value: String) {
        val error = InterestCatalog.validateLabel(value)
        if (error != null) return emit(error)
        val label = InterestCatalog.normalize(listOf(value)).firstOrNull() ?: return
        val selected = state.value!!.selected
        if (selected.any { same(it, label) })
            return emit("Already in your mix. You can remove it below.")
        if (selected.size >= InterestCatalog.MAX_INTERESTS)
            return emit("Your mix is full. Remove an interest to make room.")
        saved["selectedInterests"] = ArrayList(selected + label)
        saved["query"] = ""
        emit()
    }

    fun remove(label: String) {
        saved["selectedInterests"] = ArrayList(state.value!!.selected.filterNot { same(it, label) })
        emit()
    }

    fun toggle(label: String) {
        if (state.value!!.selected.any { same(it, label) }) remove(label) else add(label)
    }

    fun explore() {
        saved["pendingChoices"] = arrayListOf<String>()
        saved["step"] = 0
        emit()
    }

    fun choose(id: String?) {
        val step = state.value!!.step
        val question = InterestCatalog.questions.getOrNull(step) ?: return
        val answers = saved.get<ArrayList<String>>("pendingChoices").orEmpty().toMutableList()
        question.choices.firstOrNull { it.id == id }?.let { answers.add(it.id) }
        saved["pendingChoices"] = ArrayList(answers)
        if (step + 1 >= InterestCatalog.questions.size) {
            saved["choices"] = ArrayList(answers)
            saved["step"] = -1
        } else saved["step"] = step + 1
        emit()
    }

    fun cancelExploration() {
        saved["step"] = -1
        saved["pendingChoices"] = arrayListOf<String>()
        emit()
    }
}
