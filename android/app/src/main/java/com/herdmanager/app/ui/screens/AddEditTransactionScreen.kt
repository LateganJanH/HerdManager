package com.herdmanager.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.herdmanager.app.domain.model.Animal
import com.herdmanager.app.domain.model.ExpenseCategory
import com.herdmanager.app.domain.model.Transaction
import com.herdmanager.app.domain.model.TransactionType
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: String?,
    initialType: TransactionType,
    onNavigateBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val animals by viewModel.animals.collectAsState(initial = emptyList())
    val categories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    var type by remember(initialType) { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    var pricePerKgStr by remember { mutableStateOf("") }
    var dateEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var notes by remember { mutableStateOf("") }
    var animalId by remember { mutableStateOf<String?>(null) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var setAnimalSold by remember { mutableStateOf(true) }
    var showAnimalPicker by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val farmId = com.herdmanager.app.domain.model.FarmSettings.DEFAULT_FARM_ID
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val existing by viewModel.selectedTransaction.collectAsState(initial = null)

    LaunchedEffect(transactionId) {
        if (transactionId != null) viewModel.loadTransaction(transactionId)
    }
    LaunchedEffect(existing) {
        if (existing != null) {
            amountStr = (existing!!.amountCents / 100.0).toString()
            dateEpochDay = existing!!.dateEpochDay
            notes = existing!!.notes ?: ""
            type = existing!!.type
            weightStr = existing!!.weightKg?.toString() ?: ""
            pricePerKgStr = existing!!.pricePerKgCents?.let { (it / 100.0).toString() } ?: ""
            animalId = existing!!.animalId
            contactName = existing!!.contactName ?: ""
            contactPhone = existing!!.contactPhone ?: ""
            contactEmail = existing!!.contactEmail ?: ""
            categoryId = existing!!.categoryId
            description = existing!!.description ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Add transaction" else "Edit transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                isError = amountError != null,
                supportingText = {
                    amountError?.let { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (type == TransactionType.SALE || type == TransactionType.PURCHASE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pricePerKgStr,
                        onValueChange = { pricePerKgStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price per kg") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                // Derived price per kg hint when both values present
                val weightVal = weightStr.toDoubleOrNull()
                val amountVal = amountStr.toDoubleOrNull()
                if (weightVal != null && weightVal > 0 && amountVal != null && amountVal > 0) {
                    val derivedPricePerKg = amountVal / weightVal
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "≈ %.2f per kg (derived)".format(derivedPricePerKg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.ofEpochDay(dateEpochDay)),
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val current = LocalDate.ofEpochDay(dateEpochDay)
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                                dateEpochDay = newDate.toEpochDay()
                            },
                            current.year,
                            current.monthValue - 1,
                            current.dayOfMonth
                        ).show()
                    },
                readOnly = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))

            val animalOptions: List<com.herdmanager.app.domain.model.Animal> =
                if (type == TransactionType.SALE) {
                    animals.filter { it.status != com.herdmanager.app.domain.model.AnimalStatus.SOLD }
                } else {
                    animals
                }

            when (type) {
                TransactionType.SALE, TransactionType.PURCHASE -> {
                    Text("${if (type == TransactionType.SALE) "Buyer" else "Seller"} contact", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = contactName, onValueChange = { contactName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Animal", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = animalId?.let { id -> animals.find { it.id == id }?.earTagNumber ?: "" } ?: "",
                        onValueChange = {},
                        label = { Text("Select animal") },
                        modifier = Modifier
                            .fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { showAnimalPicker = true }) {
                                Text("Choose")
                            }
                        },
                        singleLine = true
                    )
                    if (type == TransactionType.SALE && animalId != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = setAnimalSold, onCheckedChange = { setAnimalSold = it })
                            Text("Set animal status to SOLD")
                        }
                    }
                }
                TransactionType.EXPENSE -> {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExpenseCategoryDropdown(
                        categories = categories,
                        selectedCategoryId = categoryId,
                        onCategorySelected = { categoryId = it }
                    )
                }
            }

            generalError?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (showAnimalPicker) {
                AlertDialog(
                    onDismissRequest = { showAnimalPicker = false },
                    title = { Text("Select animal") },
                    text = {
                        Column {
                            if (animalOptions.isEmpty()) {
                                Text("No animals available for this transaction.")
                            } else {
                                animalOptions.forEach { animal: com.herdmanager.app.domain.model.Animal ->
                                    TextButton(
                                        onClick = {
                                            animalId = animal.id
                                            showAnimalPicker = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = buildString {
                                                append(animal.earTagNumber)
                                                animal.name?.takeIf { it.isNotBlank() }?.let { append(" - ").append(it) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAnimalPicker = false }) {
                            Text("Close")
                        }
                    }
                )
            }
            Button(
                onClick = {
                    amountError = null
                    generalError = null

                    val amountCents = (amountStr.toDoubleOrNull() ?: 0.0).times(100).toLong()
                    var hasError = false
                    if (amountCents <= 0) {
                        amountError = "Enter a valid amount"
                        hasError = true
                    }

                    if ((type == TransactionType.SALE || type == TransactionType.PURCHASE) && animalId == null) {
                        generalError = "Select an animal for this transaction"
                        hasError = true
                    }

                    if (type == TransactionType.EXPENSE && description.isBlank() && categoryId == null) {
                        generalError = "Add a description or select a category"
                        hasError = true
                    }

                    if (hasError) return@Button
                    val weightKg = weightStr.toDoubleOrNull()
                    val pricePerKgCents = pricePerKgStr.toDoubleOrNull()?.let { (it * 100).toLong() }
                    val id = transactionId ?: UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val createdAt = existing?.createdAt ?: now
                    viewModel.saveTransaction(
                        Transaction(
                            id = id,
                            type = type,
                            amountCents = amountCents,
                            dateEpochDay = dateEpochDay,
                            farmId = farmId,
                            notes = notes.takeIf { it.isNotBlank() },
                            createdAt = createdAt,
                            updatedAt = now,
                            weightKg = weightKg,
                            pricePerKgCents = pricePerKgCents,
                            animalId = animalId,
                            contactName = contactName.takeIf { it.isNotBlank() },
                            contactPhone = contactPhone.takeIf { it.isNotBlank() },
                            contactEmail = contactEmail.takeIf { it.isNotBlank() },
                            categoryId = categoryId,
                            description = description.takeIf { it.isNotBlank() }
                        ),
                        setAnimalSold = setAnimalSold
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseCategoryDropdown(
    categories: List<ExpenseCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = selectedCategoryId?.let { id ->
        categories.find { it.id == id }?.name ?: "Select category"
    } ?: "Select category"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
