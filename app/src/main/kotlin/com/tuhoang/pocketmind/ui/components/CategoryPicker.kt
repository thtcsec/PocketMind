package com.tuhoang.pocketmind.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.utils.CategoryUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    onAddCustom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (selectedParent, selectedSub) = remember(selected) { CategoryUtils.parseCategory(selected) }
    var parent by remember(selectedParent) { mutableStateOf(selectedParent) }
    var sub by remember(selectedSub) { mutableStateOf(selectedSub.orEmpty()) }
    var showCustom by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    val subcategories = remember(parent) {
        if (parent.isBlank()) emptyList() else CategoryUtils.subcategoriesFor(context, parent)
    }

    fun emitSelection() {
        onSelected(CategoryUtils.formatCategory(parent, sub.takeIf { it.isNotBlank() }))
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.category_parent_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = parent == cat,
                    onClick = {
                        parent = cat
                        sub = ""
                        showCustom = false
                        emitSelection()
                    },
                    label = { Text(cat) }
                )
            }
            FilterChip(
                selected = showCustom,
                onClick = { showCustom = !showCustom },
                label = { Text(stringResource(R.string.category_add_custom)) }
            )
        }

        if (showCustom) {
            OutlinedTextField(
                value = customInput,
                onValueChange = { customInput = it },
                label = { Text(stringResource(R.string.category_custom_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    TextButton(
                        onClick = {
                            val name = customInput.trim()
                            if (name.isNotEmpty()) {
                                onAddCustom(name)
                                parent = name
                                sub = ""
                                onSelected(name)
                                customInput = ""
                                showCustom = false
                            }
                        },
                        enabled = customInput.isNotBlank()
                    ) { Text(stringResource(R.string.action_add)) }
                }
            )
        }

        if (parent.isNotBlank() && subcategories.isNotEmpty()) {
            Text(
                text = stringResource(R.string.category_detail_label, parent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subcategories.forEach { detail ->
                    FilterChip(
                        selected = sub == detail,
                        onClick = {
                            sub = detail
                            emitSelection()
                        },
                        label = { Text(detail) }
                    )
                }
            }
        }
    }
}
