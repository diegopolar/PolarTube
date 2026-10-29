package com.polartube.app.ui.player

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

fun downloadVideoSimple(context: Context, url: String?, title: String?) {
    if (url == null) {
        Toast.makeText(context, "Error: Enlace de video no disponible", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        // Limpiamos el título para evitar caracteres inválidos en el nombre del archivo
        val cleanTitle = title?.replace(Regex("[^a-zA-Z0-9.\\- ]"), "") ?: "PolarTube_Video"
        
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(cleanTitle)
            setDescription("Descargando desde PolarTube...")
            // Muestra el progreso en la barra de notificaciones del celular
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Guarda directo en la carpeta pública de Descargas
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$cleanTitle.mp4")
            // Replicamos el User-Agent para que YouTube no bloquee la descarga
            addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
        }
        
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Descarga iniciada... revisa tus notificaciones", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al iniciar la descarga", Toast.LENGTH_SHORT).show()
    }
}
