package com.woocommerce.android.ui.prefs.cardreader.hub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooTheme


class CardReaderManualsFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CardReaderManualsScreen(viewModel = CardReaderManualsViewModel())
                }
            }
        }
}

@Composable
fun ImageListItem () {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.ic_card_reader_manual),
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
        repeat(5) {
            ImageListItem()
    }
  }
}

@Composable
fun CardReaderManualsScreen(viewModel: CardReaderManualsViewModel) {
    TopAppBar() {

    }
        ManualsList()

}

@Preview
@Composable
fun ManualListPreview() {
    WooTheme {
        CardReaderManualsScreen(viewModel = CardReaderManualsViewModel())
    }
}
