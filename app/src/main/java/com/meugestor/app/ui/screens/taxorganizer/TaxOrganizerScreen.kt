package com.meugestor.app.ui.screens.taxorganizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxOrganizerScreen(app: MeuGestorApp) {
    val viewModel: TaxOrganizerViewModel = viewModel(
        factory = TaxOrganizerViewModel.Factory(app.taxRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val selectedCategory = state.categories.find { it.id == state.selectedCategoryId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header disclaimer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InfoBlue.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Este módulo é organizacional e educativo. Confirme sempre com as regras vigentes da Receita Federal ou com um contador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InfoBlue
                    )
                }
            }
        }

        // Year selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ano-Base", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row {
                    state.availableYears.forEach { year ->
                        FilterChip(
                            selected = state.selectedYear == year,
                            onClick = { viewModel.selectYear(year) },
                            label = { Text(year.toString()) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        // Progress summary
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Checklist de Documentos", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            "${(state.checklistProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.checklistProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${state.checklist.count { it.isCompleted }} de ${state.checklist.size} documentos organizados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category grid (2 columns)
        item {
            Text("Categorias de Documentos", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
        item {
            // Using a fixed-height grid within a LazyColumn item
            val rows = state.categories.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { cat ->
                            TaxCategoryCard(
                                category = cat,
                                isSelected = state.selectedCategoryId == cat.id,
                                docCount = state.documents.count { it.category.name == cat.id },
                                onSelect = {
                                    viewModel.selectCategory(if (state.selectedCategoryId == cat.id) null else cat.id)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Selected category detail
        if (selectedCategory != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${selectedCategory.emoji} ${selectedCategory.name}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(selectedCategory.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.Lightbulb, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(selectedCategory.deductibleInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            val catDocs = state.documents.filter { it.category.name == selectedCategory.id }
            if (catDocs.isNotEmpty()) {
                item {
                    Text("Documentos (${catDocs.size})", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                items(catDocs) { doc ->
                    TaxDocumentItem(doc)
                }
            } else {
                item {
                    Text("Nenhum documento nesta categoria ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            item {
                Button(
                    onClick = { viewModel.toggleAddDocDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Documento")
                }
            }
        }

        // Checklist section
        if (state.checklist.isNotEmpty()) {
            item {
                Text("Checklist Completo", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            items(state.checklist) { item ->
                ChecklistItemRow(
                    item = item,
                    onToggle = { viewModel.toggleChecklistItem(it) }
                )
            }
        }
    }

    if (state.showAddDocDialog && state.selectedCategoryId != null) {
        AddDocumentDialog(
            categoryId = state.selectedCategoryId!!,
            year = state.selectedYear,
            onDismiss = { viewModel.toggleAddDocDialog() },
            onAdd = { viewModel.addDocument(it) }
        )
    }
}

@Composable
private fun TaxCategoryCard(
    category: TaxCategory,
    isSelected: Boolean,
    docCount: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(category.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(category.name, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium, maxLines = 2)
            if (docCount > 0) {
                Badge { Text("$docCount") }
            }
        }
    }
}

@Composable
private fun TaxDocumentItem(doc: com.meugestor.app.data.database.entity.TaxDocumentEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.description, style = MaterialTheme.typography.bodyMedium)
                Text(doc.source ?: "", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (doc.amount != null) {
                    Text(CurrencyUtils.formatBRL(doc.amount), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                }
                if (doc.hasAttachment) {
                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: com.meugestor.app.data.database.entity.TaxChecklistItemEntity,
    onToggle: (com.meugestor.app.data.database.entity.TaxChecklistItemEntity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isCompleted, onCheckedChange = { onToggle(item) })
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )
            if (!item.notes.isNullOrBlank()) {
                Text(item.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDocumentDialog(
    categoryId: String,
    year: Int,
    onDismiss: () -> Unit,
    onAdd: (com.meugestor.app.data.database.entity.TaxDocumentEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Documento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Descrição") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = source, onValueChange = { source = it },
                    label = { Text("Fonte / Empresa") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Valor (R$) — opcional") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(com.meugestor.app.data.database.entity.TaxDocumentEntity(
                        year = year,
                        category = com.meugestor.app.data.database.entity.TaxDocumentCategory.valueOf(categoryId),
                        description = description,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        source = source.ifBlank { "—" },
                        documentDate = com.meugestor.app.util.DateUtils.today(),
                        createdAt = com.meugestor.app.util.DateUtils.today()
                    ))
                },
                enabled = description.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
