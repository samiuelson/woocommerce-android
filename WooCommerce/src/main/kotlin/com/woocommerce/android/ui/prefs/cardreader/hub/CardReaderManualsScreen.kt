package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ManualsScreen(
    modifier: Modifier = Modifier,
    cardReaderManualsViewModel: CardReaderManualsViewModel = viewModel()
) {

    ManualsList(
        list = cardReaderManualsViewModel.manualState
    )
}

@Composable
fun ManualListItem(
    manualLabel: String,
    manualIcon: Int,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onManualClick }

    ) {
        Image(
            painterResource(manualIcon),
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(text = manualLabel)
        }
    }
}

@Composable
fun ManualsList (
    list: List<ManualItem>
) {
    LazyColumn(
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)

    ) {
        items (
            items = list
        ) { manual ->
            ManualListItem(
                manualLabel = manual.label,
                manualIcon = manual.icon,
                onManualClick = { /*TODO*/ })
        }
    }
}


