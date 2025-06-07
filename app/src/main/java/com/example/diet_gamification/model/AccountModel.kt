package com.example.diet_gamification.model

import android.os.Parcel
import android.os.Parcelable

data class AccountModel(
    var id: Int,
    var email: String,
    var name: String,
    var password: String,
    var gender: String,
    var exp: Int,
    var berat: Int,
    var tinggi: Int,
    var inventory: String? = null,
    var setting: String? = null,
    var is_verify: Boolean = false,
    var created_at: String = "",
    var updated_at: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(email)
        parcel.writeString(name)
        parcel.writeString(password)
        parcel.writeString(gender)
        parcel.writeInt(exp)
        parcel.writeInt(berat)
        parcel.writeInt(tinggi)
        parcel.writeString(inventory)
        parcel.writeString(setting)
        parcel.writeByte(if (is_verify) 1 else 0)
        parcel.writeString(created_at)
        parcel.writeString(updated_at)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AccountModel> {
        override fun createFromParcel(parcel: Parcel): AccountModel {
            return AccountModel(parcel)
        }

        override fun newArray(size: Int): Array<AccountModel?> {
            return arrayOfNulls(size)
        }
    }
}
