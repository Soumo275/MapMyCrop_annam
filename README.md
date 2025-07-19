# MapMyCrop: Smart Acreage &amp; Yield Estimation

## Parts:

### 1. [WebApp Description](#webapp-description)

### 2. [Java Swing Tool](#java-swing-tool)

# Webapp description

A web application built with Flask that allows users to upload `.tiff` satellite images, generates thumbnails, and stores image URLs to Supabase.

---

## 1. WebApp Features

- ✅ Upload `.tiff` satellite images
- ✅ Generate thumbnails using Rasterio
- ✅ Run deep learning inference with a trained model
- ✅ Convert inference output to `.png` and store to Supabase
- ✅ Save result URLs in PostgreSQL (via SQLAlchemy)
- ✅ Serve static files and results with Flask routes

---

## Web app Tech Stack

- **Backend:** Flask, SQLAlchemy, Supabase, Rasterio, NumPy
- **ML/Inference:** Keras, TensorFlow
- **Others:** GDAL, Docker, dotenv, Pillow
- **Frontend:** HTML, CSS, Leaflet.js , OpenStreetMaps

## Installation & Setup

### 1. Set Up Supabase Storage and Database

#### Create a Supabase Bucket (or any object or block storage )

1. Go to [https://app.supabase.com/](https://app.supabase.com/) and open your project.
2. Navigate to **Storage > Buckets**.
3. Click **"New bucket"** and enter a name
4. Make sure to check **"Public"** if you want direct access to uploaded files via URL.
5. Use this bucket name in your `.env` file:

---

#### Create a Relational Database

You can use **PostgreSQL**, **SQLite**, or any SQL-compatible database. Below are instructions :

##### PostgreSQL (Recommended)

1. Use a PostgreSQL service Supabase, Railway, Render, etc.
2. Create a new database called `geo_tiff`.
3. Copy the connection string (URL) and use it in your `.env`:

---

### 2. Install Requirements

```bash
pip install -r requirements.txt
```

---

### 3. Setup Environment Variables

Create a `.env` file in the `app` folder:

```env
SUPABASE_URL= https://your-project.supabase.co
SUPABASE_KEY= your-private-key
SUPABASE_BUCKET= your bucket name

DB_USER= your user id
DB_PASSWORD= database password
DB_HOST= host name from database
DB_NAME= postgres

```

---

### 5. Initialize the Database (First-Time Only)

In a Python shell, run the following to create the database tables:

```python
from app import db
db.create_all()
```

---

## Running the App

Run the Flask app with:

```bash
python app/app.py
```

The app will be accessible at `http://localhost:5000/`.

# 2. Java Swing Tool

A standalone desktop application developed in **Java** for estimating agricultural yield based on user-supplied yield values and classified acreage data.

## Overview

This tool provides a simple and intuitive interface to:

- Input classified area data (in binary format)
- Enter user-supplied yield values
- Estimate total yield based on area × yield
- View and export results

It is designed to assist in agricultural planning by processing structured input data to produce fast, offline yield calculations.

---

## Features

- Built with **Java Swing** and **AWT**
- Compatible with **binary input data** formats
- Simple input/output interface
- Fast and offline processing
- Platform-independent — runs on any machine with Java installed

---

## Getting Started

### Prerequisites

- **Java JDK 8 or higher**  
  Download: [Oracle Java](https://www.oracle.com/java/technologies/javase-downloads.html)

### Running the Tool

```bash
javac CropYieldGUI.java
java CropYieldGUI
```
