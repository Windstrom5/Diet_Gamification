package com.example.diet_gamification.model

import android.os.Parcel
import android.os.Parcelable


data class AccountModel (
    var id: Int,
    var email: String,
    var name: String,
    var password: String,
    var Gender: String,
    var Exp: Int,
    var berat: Int,
    var tinggi: Int,
    var inventory: String? = null,
    var setting: String?= null
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
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeValue(email)
        parcel.writeString(name)
        parcel.writeString(password)
        parcel.writeString(Gender)
        parcel.writeInt(Exp)
        parcel.writeInt(berat)
        parcel.writeInt(tinggi)
        parcel.writeString(inventory)
        parcel.writeString(setting)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AccountModel> {
        override fun createFromParcel(parcel: Parcel): AccountModel {
            return AccountModel(parcel)
        }

        override fun newArray(size: Int): Array<AccountModel?> {
            return arrayOfNulls(size)
        }
    }
}