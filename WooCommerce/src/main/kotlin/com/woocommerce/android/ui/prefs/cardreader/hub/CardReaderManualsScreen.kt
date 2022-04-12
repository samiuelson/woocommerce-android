package com.woocommerce.android.ui.prefs.cardreader.hub


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun CardReaderManualsScreen(viewModel: CardReaderManualsViewModel) {
    TopAppBar() {
    }
    ManualsList()
}

@Composable
fun ImageListItem () {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.ic_p400),
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text("Card Reader", style = MaterialTheme.typography.subtitle1)
    }
}

@Composable
fun ManualsList (modifier: Modifier = Modifier) {

    Column() {
        repeat(2) {
            ImageListItem()
        }
    }
}
//@Preview
//@Composable
//fun ManualListPreview() {
//    WooTheme {
//        CardReaderManualsScreen(viewModel = CardReaderManualsViewModel())
//    }
//}
