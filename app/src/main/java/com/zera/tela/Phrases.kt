package com.zera.tela

import android.content.Context
import kotlin.random.Random

object Phrases {
    val list = listOf(
        "Seu tempo é a única coisa que você não consegue recomprar. Use-o com intenção.",
        "Cada minuto longe da tela é um minuto devolvido à sua vida.",
        "Você não perdeu o acesso. Você ganhou a tarde de volta.",
        "As pequenas escolhas de hoje constroem a pessoa que você quer ser amanhã.",
        "O que você faria agora se esse app nem existisse? Vá fazer isso.",
        "Disciplina é escolher o que você mais quer, e não o que você quer agora.",
        "Levante, respire fundo e olhe ao redor. A vida real está acontecendo agora.",
        "Seus objetivos merecem mais atenção do que o próximo vídeo.",
        "Você combinou esse limite consigo mesmo. Cumprir a própria palavra é ter respeito por si.",
        "Descansar os olhos, a mente e os dedos também é produtividade.",
        "Ligue para alguém de quem você gosta. Uma conversa vale mais que mil rolagens.",
        "Quem controla o tempo controla a própria vida. Hoje, esse alguém é você.",
        "Um passo, uma página, um treino, um copo d'água. Qualquer coisa vale mais que mais cinco minutos de tela.",
        "A atenção é o seu bem mais valioso. Não a entregue de graça.",
        "O dia ainda tem boas horas. Aproveite-as fora do celular.",
        "Amanhã você vai agradecer por ter parado agora.",
        "Foco não é ausência de distração. É a decisão de voltar para o que importa.",
        "Que tal aprender algo novo hoje? Dez minutos de dedicação já contam.",
        "Sono, saúde e relações melhoram quando o tempo de tela diminui.",
        "Você é mais forte que o hábito. Mostre isso agora."
    )

    /** Sorteia uma frase, nunca repetindo a última exibida. */
    fun next(c: Context): String {
        val last = Store.lastPhrase(c)
        var i: Int
        do { i = Random.nextInt(list.size) } while (i == last && list.size > 1)
        Store.setLastPhrase(c, i)
        return list[i]
    }
}
