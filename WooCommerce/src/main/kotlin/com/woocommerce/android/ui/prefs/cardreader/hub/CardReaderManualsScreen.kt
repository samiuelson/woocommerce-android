package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground


@Composable
fun CardReaderManualsScreen(viewModel: CardReaderManualsViewModel) {
    val manualListState by viewModel.manualState.observeAsState(CardReaderManualsViewModel.ManualsListState())

    CardReaderManualsScreen(
        state = manualListState,
        onManualClick = viewModel::onManualClick
    )
}

@Composable
fun CardReaderManualsScreen(
    state: CardReaderManualsViewModel.ManualsListState,
    onManualClick: (String) -> Unit
) {
    CardReaderManualsList()
}

@Composable
fun CardReaderManualsList () {
    Column {
        repeat(100) {
            ManualListItem( )
        }
    }
}

//@Composable
//fun CardReaderManualsList(
//    manuals: List<CardReaderManualsViewModel.ManualsListItem>,
//    onManualClick: (String) -> Unit
//) {
//    LazyColumn(
//        verticalArrangement = Arrangement.spacedBy(0.dp),
//        modifier = Modifier
//            .background(color = MaterialTheme.colors.surface)
//    ) {
//        itemsIndexed(manuals) { index, manual ->
//            CardReaderManualsViewModel.ManualsListItem(
//                manual = manual,
//                onManualClick = onManualClick,
//                label = R.string.bbpos_reader,
//                icon = R.drawable.ic_bbposchipper
//            )
//            if (index < manuals.lastIndex) {
//            Divider(
//                modifier = Modifier
//                    .offset(x = 16.dp),
//                color = colorResource(id = R.color.divider_color),
//                thickness = 1.dp
//            )
//        }
//        }
//    }
//}

@Composable
fun ManualListItem(
//    manual: CardReaderManualsViewModel.ManualsListItem,
//    onManualClick: (String) -> Unit,
//    label: Int,
//    icon: Int
) {
    Column() {
        Image(
            painter = painterResource(id = R.drawable.ic_card_reader_manual),
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text("Card Reader", style = MaterialTheme.typography.subtitle1)
    }
}

//@Composable
//fun ManualsListItem(
//    manual: CardReaderManualsViewModel.ManualsListItem,
//    onManualClick: (String) -> Unit,
//    label: Int,
//    icon: Int
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 8.dp)
//            .clickable(
//                enabled = true,
//                onClickLabel = stringResource(id = R.string.card_reader_manuals),
//                role = Role.Button,
//                onClick = { onManualClick }
//            )
//
//    ) {
//        Image(
//            painter = painterResource(icon),
//            contentDescription = null,
//            modifier = Modifier.size(50.dp)
//        )
//        Spacer(Modifier.width(10.dp))
//        Text(label.toString())
//    }
//}
//@Preview
//@Composable
//fun CardReaderManualsScreenPreview() {
//    WooThemeWithBackground {
//        CardReaderManualsScreen(viewModel = CardReaderManualsViewModel())
//    }
//}
