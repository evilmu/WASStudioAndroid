package br.com.wasstudio

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.widget.*
import android.graphics.Color
import java.io.*
import java.util.zip.*

class MainActivity : Activity() {
    private val main = "animation/animation.json"
    private val secondary = "animation/animation_secondary.json"
    private var primary: LinkedHashMap<String, ByteArray>? = null
    private var effects: LinkedHashMap<String, ByteArray>? = null
    private var output: ByteArray? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 20)
            setBackgroundColor(Color.rgb(246, 246, 250))
        }
        fun button(label: String, action: () -> Unit) {
            layout.addView(Button(this).apply { text = label; setOnClickListener { action() } })
        }
        layout.addView(TextView(this).apply {
            text = "WAS Studio\nCombine personagens e efeitos .was"
            textSize = 23f
            setPadding(0, 0, 0, 28)
        })
        button("Selecionar figurinha principal") { pick(10) }
        button("Selecionar figurinha com efeito") { pick(11) }
        button("Montar figurinha híbrida") { build() }
        button("Salvar .was") {
            if (output == null) { toast("Monte a figurinha primeiro"); return@button }
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "WASStudio_hibrido.was")
            }, 12)
        }
        status = TextView(this).apply { text = "Aguardando arquivos"; textSize = 16f }
        layout.addView(status)
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun pick(code: Int) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }, code)
    }

    @Deprecated("Legacy activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        try {
            if (requestCode == 12) {
                val bytes = output ?: return
                contentResolver.openOutputStream(data.data!!)?.use { it.write(bytes) }
                    ?: error("Falha ao gravar")
                toast("Arquivo salvo")
                return
            }
            val bytes = contentResolver.openInputStream(data.data!!)?.use { it.readBytes() }
                ?: error("Arquivo inacessível")
            val entries = unpack(bytes)
            require(entries.containsKey(main) && entries.containsKey("$main.trust_token")) {
                "Animação principal ou token ausente"
            }
            if (requestCode == 10) primary = entries else {
                require(entries.containsKey(secondary) && entries.containsKey("$secondary.trust_token")) {
                    "Efeito secundário ou token ausente"
                }
                effects = entries
            }
            output = null
            status.text = "Principal: ${if(primary == null) "pendente" else "OK"} | Efeito: ${if(effects == null) "pendente" else "OK"}"
        } catch (e: Exception) { toast(e.message ?: "Erro ao abrir") }
    }

    private fun unpack(data: ByteArray): LinkedHashMap<String, ByteArray> {
        require(data.size <= 30_000_000) { "Arquivo muito grande" }
        val result = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(data)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.name.contains("..") && !entry.name.startsWith("/")) { "Entrada ZIP inválida" }
                if (!entry.isDirectory) {
                    require(result.size < 200) { "Muitas entradas ZIP" }
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    while (true) {
                        val n = zip.read(chunk)
                        if (n < 0) break
                        buffer.write(chunk, 0, n)
                        require(buffer.size() <= 10_000_000) { "Entrada ZIP muito grande" }
                    }
                    result[entry.name] = buffer.toByteArray()
                }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun build() {
        try {
            val p = primary ?: error("Selecione a figurinha principal")
            val e = effects ?: error("Selecione a figurinha com efeito")
            val combined = LinkedHashMap(p)
            combined[secondary] = e[secondary] ?: error("Efeito ausente")
            combined["$secondary.trust_token"] = e["$secondary.trust_token"] ?: error("Token ausente")
            val stream = ByteArrayOutputStream()
            ZipOutputStream(stream).use { zip ->
                for ((name, bytes) in combined) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output = stream.toByteArray()
            status.text = "Figurinha híbrida pronta (${output!!.size} bytes)"
        } catch (e: Exception) { toast(e.message ?: "Erro") }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
