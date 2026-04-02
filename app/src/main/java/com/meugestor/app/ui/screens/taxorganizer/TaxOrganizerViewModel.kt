package com.meugestor.app.ui.screens.taxorganizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TaxChecklistItemEntity
import com.meugestor.app.data.database.entity.TaxDocumentEntity
import com.meugestor.app.data.repository.TaxRepository
import com.meugestor.app.util.DateUtils
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
    val availableYears: List<Int> = listOf(DateUtils.currentYear() - 1, DateUtils.currentYear() - 2, DateUtils.currentYear() - 3),
    val categories: List<TaxCategory> = emptyList(),
    val documents: List<TaxDocumentEntity> = emptyList(),
    val checklist: List<TaxChecklistItemEntity> = emptyList(),
    val checklistProgress: Float = 0f,
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = true,
    val showAddDocDialog: Boolean = false
)

class TaxOrganizerViewModel(
    private val taxRepository: TaxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxOrganizerUiState(categories = buildCategories()))
    val uiState: StateFlow<TaxOrganizerUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        val year = _uiState.value.selectedYear
        viewModelScope.launch {
            taxRepository.getDocumentsByYear(year).collect { docs ->
                _uiState.update { it.copy(documents = docs, isLoading = false) }
            }
        }
        viewModelScope.launch {
            taxRepository.getChecklist(year).collect { items ->
                val completed = items.count { it.isCompleted }
                val progress = if (items.isNotEmpty()) completed.toFloat() / items.size else 0f
                _uiState.update { it.copy(checklist = items, checklistProgress = progress) }
            }
        }
    }

    fun selectYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year, isLoading = true) }
        loadData()
    }

    fun selectCategory(id: String?) {
        _uiState.update { it.copy(selectedCategoryId = id) }
    }

    fun addDocument(doc: TaxDocumentEntity) {
        viewModelScope.launch {
            taxRepository.addDocument(doc)
            _uiState.update { it.copy(showAddDocDialog = false) }
        }
    }

    fun toggleChecklistItem(item: TaxChecklistItemEntity) {
        viewModelScope.launch {
            taxRepository.toggleChecklistItem(item.id, !item.isCompleted)
        }
    }

    fun toggleAddDocDialog() {
        _uiState.update { it.copy(showAddDocDialog = !it.showAddDocDialog) }
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
