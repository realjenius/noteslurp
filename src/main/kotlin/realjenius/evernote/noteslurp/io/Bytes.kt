package realjenius.evernote.noteslurp.io

import java.security.MessageDigest


fun md5(bytes: ByteArray) = MessageDigest.getInstance("MD5").digest(bytes)

fun byteArrayToHex(array: ByteArray) = array.joinToString("") { byteString(it) }

private fun byteString(byte: Byte) = Integer.toHexString((byte.toInt() or 0x100) and 0x1ff)
  .toUpperCase()
  .substring(1)
