package dev.alexissdev.kronos.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza todos los mensajes de texto del plugin Kronos HCF que se muestran a los jugadores.
 *
 * <p>Los mensajes se indexan por claves jerárquicas (p. ej. {@code "economy.balance-self"}) y
 * admiten códigos de color con prefijo {@code &} que se traducen al formato de sección de
 * Minecraft. También permite sustituir marcadores de posición ({@code {placeholder}}) con
 * valores dinámicos en tiempo de ejecución mediante {@link #format(String, Object...)}.</p>
 *
 * <p>El mapa de mensajes es {@code volatile} para permitir recargas en caliente a través de
 * {@link #reload(Map)} sin necesidad de reiniciar el servidor, garantizando visibilidad entre hilos.</p>
 */
public final class MessagesConfig {

    private volatile Map<String, String> messages;

    /**
     * Crea una nueva instancia con el mapa de mensajes inicial cargado desde la configuración del plugin.
     *
     * @param messages mapa de clave-valor donde la clave es el identificador del mensaje
     *                 y el valor es el texto (con posibles códigos de color {@code &})
     */
    public MessagesConfig(Map<String, String> messages) {
        this.messages = messages;
    }

    /**
     * Reemplaza atómicamente el mapa de mensajes con uno nuevo, efectuando una recarga en caliente.
     * Se crea una copia defensiva del mapa recibido para evitar modificaciones externas accidentales.
     *
     * @param newMessages nuevo mapa de mensajes cargado desde el archivo de configuración
     */
    public void reload(Map<String, String> newMessages) {
        this.messages = new LinkedHashMap<>(newMessages);
    }

    /**
     * Obtiene el mensaje correspondiente a la clave dada, con los códigos de color ya procesados.
     * Si la clave no existe en el mapa, retorna un marcador de error visible en el juego para
     * facilitar la detección de mensajes faltantes durante el desarrollo.
     *
     * @param key identificador del mensaje (p. ej. {@code "economy.player-not-found"})
     * @return texto del mensaje con colores procesados, o {@code "&c[msg:<key>]"} si no se encuentra
     */
    public String get(String key) {
        return colorize(messages.getOrDefault(key, "&c[msg:" + key + "]"));
    }

    /**
     * Obtiene un mensaje y sustituye los marcadores de posición con los valores proporcionados.
     *
     * <p>Los marcadores tienen la forma {@code {nombre}} y los pares clave-valor se pasan como
     * argumentos varargs intercalados: {@code format("clave", "nombre", valor, "otro", valor2)}.</p>
     *
     * <p>Ejemplo de uso:
     * <pre>{@code
     * messages.format("economy.balance-self", "amount", "100.00")
     * // Sustituye {amount} por "100.00" en el mensaje de la clave "economy.balance-self"
     * }</pre>
     * </p>
     *
     * @param key   identificador del mensaje base
     * @param pairs pares alternados de (nombre_marcador, valor_reemplazo); el número de argumentos debe ser par
     * @return mensaje con los marcadores reemplazados y los colores procesados
     */
    public String format(String key, Object... pairs) {
        String msg = get(key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            msg = msg.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return msg;
    }

    /**
     * Traduce los códigos de color con prefijo {@code &} al carácter de sección {@code §} que
     * Minecraft interpreta para aplicar colores y estilos en el chat.
     * Soporta todos los colores estándar (0-9, a-f) y formatos (k, l, m, n, o, r).
     *
     * @param text texto con códigos de color de prefijo {@code &}
     * @return texto con los caracteres {@code &} reemplazados por {@code §} donde corresponda
     */
    private static String colorize(String text) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&') {
                char c = chars[i + 1];
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) >= 0) {
                    chars[i] = '§';
                    chars[i + 1] = Character.toLowerCase(c);
                    i++;
                }
            }
        }
        return new String(chars);
    }
}
