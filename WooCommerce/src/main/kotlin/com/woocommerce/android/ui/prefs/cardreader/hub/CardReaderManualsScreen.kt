package com.woocommerce.android.ui.prefs.cardreader.hub

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R


@Composable
fun CardReaderManualsScreen(viewModel: CardReaderManualsViewModel) {
    val manualListState by viewModel.manualState.observeAsState(
        CardReaderManualsViewModel.ManualsListState()
    )

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
    ManualsList(
        manuals = state.manuals,
        onManualClick = onManualClick
    )
}

@Composable
fun ManualsList(
    manuals: List<CardReaderManualsViewModel.ManualsListItem>,
    onManualClick: (String) -> Unit
) {
    LazyColumn()
    {
        itemsIndexed(manuals) { index, manual ->
            ManualListItem(manual = manual, onManualClick = onManualClick)
        }
    }
}

@Composable
fun ManualListItem(
    manual: CardReaderManualsViewModel.ManualsListItem,
    onManualClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = R.string.p400_reader),
                role = Role.Button,
                onClick = { onManualClick }
            )
    ) {
        Image(
            painter = painterResource(manual.icon),
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text("blblablaba")
        }

    }
}

//@Composable
//fun CardReaderManualsScreen (viewModel: CardReaderManualsViewModel) {
//    val manualListState by viewModel.manualState.observeAsState(CardReaderManualsViewModel.ManualsListState())
//    CardReaderManualsScreen()
//}
//
//@Composable
//fun CardReaderManualsScreen () {
//    WooTheme {
//        ManualsList()
//    }
//}
//
//@Composable
//fun ManualsList () {
//    Column(
//    ) {
//        repeat(3) {
//            ManualListItem()
//        }
//    }
//}
//
//@Composable
//fun ManualListItem() {
//    Row (
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 8.dp)
//            .clickable(
//                enabled = true,
//                onClickLabel = stringResource(id = R.string.p400_reader),
//                role = Role.Button,
//                onClick = { }
//            )
//    ) {
//        Image(
//            painter = painterResource(R.drawable.ic_p400),
//            contentDescription = null )
//        Column (
//            modifier = Modifier
//                .padding(horizontal = 16.dp)
//                .align(Alignment.CenterVertically)
//        ){
//            Text("Test text" )
//        }
//
//    }
//}
