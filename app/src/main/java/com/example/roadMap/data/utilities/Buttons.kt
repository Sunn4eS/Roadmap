package com.example.roadMap.data.utilities

import android.content.Context
import android.content.Intent
import android.media.tv.AdRequest
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.test.R

@Composable
fun AttachFileButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_attach_file),
            contentDescription = "attach_file"
        )
    }
}