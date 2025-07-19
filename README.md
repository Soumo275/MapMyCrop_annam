# MapMyCrop_annam

# MapMyCrop: Smart Acreage &amp; Yield Estimation

A web application built with Flask that allows users to upload `.tiff` satellite images, generates thumbnails, performs deep learning inference using a `.h5` model, and stores result image URLs to Supabase.

---

## Features

- ✅ Upload `.tiff` satellite images
- ✅ Generate thumbnails using Rasterio
- ✅ Run deep learning inference with a trained model
- ✅ Convert inference output to `.png` and store to Supabase
- ✅ Save result URLs in PostgreSQL (via SQLAlchemy)
- ✅ Serve static files and results with Flask routes

---

## Tech Stack

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
