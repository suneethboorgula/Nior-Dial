package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneEnabled
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedCardBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite

@Composable
fun DefaultDialerBanner(
  onSetDefaultClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(CharcoalSurface)
      .border(1.dp, ElevatedCardBorder, RoundedCornerShape(16.dp))
      .clickable(onClick = onSetDefaultClick)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("default_dialer_banner"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(GraphiteSurface),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.PhoneEnabled,
          contentDescription = null,
          tint = PureWhite,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = stringResource(R.string.default_dialer_banner_title),
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = stringResource(R.string.default_dialer_banner_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MutedSlate,
        )
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(PureWhite)
        .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
      Text(
        text = stringResource(R.string.set_default),
        style = MaterialTheme.typography.labelLarge,
        color = OledBlack,
      )
    }
  }
}

@Composable
fun PermissionRequestCard(
  title: String,
  description: String,
  icon: ImageVector = Icons.Rounded.Security,
  buttonTag: String = "grant_permission_button",
  onGrantClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(CharcoalSurface)
      .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(GraphiteSurface),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = PureWhite,
        modifier = Modifier.size(24.dp),
      )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = PureWhite,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MutedSlate,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = onGrantClick,
      colors = ButtonDefaults.buttonColors(
        containerColor = PureWhite,
        contentColor = OledBlack,
      ),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag(buttonTag),
    ) {
      Text(
        text = stringResource(R.string.grant_permission),
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}
