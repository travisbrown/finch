package io.finch

import shapeless.Witness

package object codec {
  type JsonType = Witness.`"application/json"`.T
  type TextEncoder[A] = Encoder.Aux[A, Witness.`"text/plain"`.T]
  type JsonEncoder[A] = Encoder.Aux[A, Witness.`"application/json"`.T]
  type BinaryEncoder[A] = Encoder.Aux[A, Witness.`"application/octet-stream"`.T]

  type JsonAnyEncoder = AnyEncoder.Aux[Witness.`"application/json"`.T]
}
