package com.meugestor.app.ui.screens.taxorganizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TaxChecklistItemEntity
import com.meugestor.app.data.database.entity.TaxDocumentEntity
import com.meugestor.app.data.repository.TaxRepository
import com.meugestor.app.util.DateUtils
import com.meugestor.app.util.SampleData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TaxCategory(
    val id: String,
    val name: String,
    val description: String,
    val deductibleInfo: String,
    val emoji: String
)

data class TaxOrganizerUiState(
    val selectedYear: Int = DateUtils.currentYear() - 1,
    val availableYears: List<Int> = defaultYears(),
    val categories: List<TaxCategory> = emptyList(),
    val documents: List<TaxDocumentEntity> = emptyList(),
    val documentsTotal: Double = 0.0,
    val checklist: List<TaxChecklistItemEntity> = emptyList(),
    val checklistProgress: Float = 0f,
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = true,
    val showAddDocDialog: Boolean = false
)

/** Ano corrente (organização durante o ano) e os três anos-base anteriores. */
private fun defaultYears(): List<Int> {
    val current = DateUtils.currentYear()
    return listOf(current, current - 1, current - 2, current - 3)
}

private data class TaxUiOptions(
    val selectedCategoryId: String? = null,
    val showAddDocDialog: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaxOrganizerViewModel(
    private val taxRepository: TaxRepository
) : ViewModel() {

    private val categories = buildCategories()
    private val selectedYear = MutableStateFlow(DateUtils.currentYear() - 1)
    private val options = MutableStateFlow(TaxUiOptions())

    // flatMapLatest cancela a coleta do ano anterior. Antes cada troca de ano iniciava novos
    // coletores sem cancelar os antigos, e os dados de anos diferentes se sobrescreviam.
    private val yearData = selectedYear.flatMapLatest { year ->
        combine(
            taxRepository.getDocumentsByYear(year),
            taxRepository.getChecklist(year)
        ) { docs, checklist -> Triple(year, docs, checklist) }
    }

    val uiState: StateFlow<TaxOrganizerUiState> = combine(yearData, options) { (year, docs, checklist), opts ->
        val completed = checklist.count { it.isCompleted }
        TaxOrganizerUiState(
            selectedYear = year,
            categories = categories,
            documents = docs,
            documentsTotal = docs.sumOf { it.amount },
            checklist = checklist,
            checklistProgress = if (checklist.isNotEmpty()) completed.toFloat() / checklist.size else 0f,
            selectedCategoryId = opts.selectedCategoryId,
            isLoading = false,
            showAddDocDialog = opts.showAddDocDialog
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TaxOrganizerUiState(categories = categories)
    )

    init {
        ensureChecklist(selectedYear.value)
    }

    fun selectYear(year: Int) {
        selectedYear.value = year
        ensureChecklist(year)
    }

    fun selectCategory(id: String?) {
        options.update { it.copy(selectedCategoryId = id) }
    }

    fun addDocument(doc: TaxDocumentEntity) {
        viewModelScope.launch {
            taxRepository.addDocument(doc)
            options.update { it.copy(showAddDocDialog = false) }
        }
    }

    fun deleteDocument(doc: TaxDocumentEntity) {
        viewModelScope.launch { taxRepository.deleteDocument(doc) }
    }

    fun toggleChecklistItem(item: TaxChecklistItemEntity) {
        viewModelScope.launch {
            taxRepository.toggleChecklistItem(item.id, !item.isCompleted)
        }
    }

    fun toggleAddDocDialog() {
        options.update { it.copy(showAddDocDialog = !it.showAddDocDialog) }
    }

    /** O checklist padrão existia em SampleData mas nunca era criado. */
    private fun ensureChecklist(year: Int) {
        viewModelScope.launch {
            taxRepository.ensureChecklist(year, SampleData.sampleTaxChecklist)
        }
    }

    private companion object {
        fun buildCategories() = listOf(
            TaxCategory("RENDIMENTOS_TRIBUTAVEIS","Rendimentos Tributáveis","Salários, pró-labore, aluguéis","Tributados na tabela progressiva","💰"),
            TaxCategory("RENDIMENTOS_ISENTOS","Rendimentos Isentos","Poupança, LCI, LCA, dividendos","Não tributados mas devem ser declarados","🏦"),
            TaxCategory("RENDIMENTOS_EXCLUSIVOS","Tributação Exclusiva","13º salário, renda fixa, PLR","Tributados exclusivamente na fonte","📋"),
            TaxCategory("DESPESAS_MEDICAS","Despesas Médicas","Consultas, exames, plano de saúde, dentista","Dedutíveis sem limite — guarde todos os recibos!","🏥"),
            TaxCategory("EDUCACAO","Educação","Ensino infantil, fundamental, médio, superior","Limite de dedução por dependente/titular (verificar valor vigente)","📚"),
            TaxCategory("PREVIDENCIA","Previdência","INSS, PGBL","PGBL: dedutível até 12% da renda bruta. VGBL: não dedutível","🔒"),
            TaxCategory("DEPENDENTES","Dependentes","Filhos, cônjuge, pais (sob condições)","Dedução fixa por dependente (verificar valor vigente)","👨‍👩‍👧‍👦"),
            TaxCategory("PENSAO","Pensão Alimentícia","Pensão judicial ou por acordo homologado","Dedutível integralmente quando judicial","⚖️"),
            TaxCategory("BENS_DIREITOS","Bens e Direitos","Imóveis, veículos, investimentos","Declarar com valor de aquisição em 31/12","🏠"),
            TaxCategory("DIVIDAS","Dívidas e Ônus","Financiamentos, empréstimos acima de R$ 5.000","Declarar com saldo devedor","💳"),
            TaxCategory("DESPESAS_PROFISSIONAIS","Despesas Profissionais","Livro-caixa de autônomos e profissionais liberais","Dedutíveis quando escrituradas em livro-caixa","💼"),
            TaxCategory("DOACOES","Doações","Doações incentivadas (ECA, idoso, cultura, esporte)","Dedutíveis até 6% do imposto devido","🤝"),
            TaxCategory("INVESTIMENTOS","Investimentos","Ações, fundos, CDB, tesouro direto","Informar posição em 31/12 e ganhos/perdas","📈"),
            TaxCategory("ALUGUEL","Aluguel","Aluguel pago ou recebido","Recebido: tributável. Pago: dedutível em livro-caixa","🏢"),
            TaxCategory("MOVIMENTACOES_BANCARIAS","Movimentações Bancárias","Movimentações relevantes em contas","Bancos informam à Receita — valores devem ser consistentes","🏧")
        )
    }

    class Factory(
        private val taxRepository: TaxRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaxOrganizerViewModel(taxRepository) as T
    }
}
