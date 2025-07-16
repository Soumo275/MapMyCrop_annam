from flask import Flask, request, jsonify, send_file, url_for
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from dotenv import load_dotenv
from supabase import create_client, Client
import rasterio
from rasterio.plot import reshape_as_image
import numpy as np
from io import BytesIO
import os
from datetime import datetime

load_dotenv()

app = Flask(__name__)
CORS(app)

# Supabase Storage config
SUPABASE_URL = os.getenv('SUPABASE_URL')
SUPABASE_KEY = os.getenv('SUPABASE_KEY')
SUPABASE_BUCKET = os.getenv('SUPABASE_BUCKET')
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Database config
DB_USER = os.getenv('DB_USER')
DB_PASSWORD = os.getenv('DB_PASSWORD')
DB_HOST = os.getenv('DB_HOST')
DB_NAME = os.getenv('DB_NAME')

app.config['SQLALCHEMY_DATABASE_URI'] = f'postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:5432/{DB_NAME}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['MAX_CONTENT_LENGTH'] = 100 * 1024 * 1024
app.config['ALLOWED_EXTENSIONS'] = {'tif', 'tiff'}

db = SQLAlchemy(app)

class GeoTIFF(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    filename = db.Column(db.String(255), unique=True, nullable=False)
    tiff_url = db.Column(db.String(512), nullable=False)
    thumbnail_url = db.Column(db.String(512), nullable=False)
    uploaded_at = db.Column(db.DateTime, default=datetime.utcnow)
    labels_url = db.Column(db.String(512), nullable=True)
    result_url = db.Column(db.String(512), nullable=True)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

def upload_to_supabase_storage(file_bytes, filename):
    response = supabase.storage.from_(SUPABASE_BUCKET).upload(filename, file_bytes)
    url = supabase.storage.from_(SUPABASE_BUCKET).get_public_url(filename)
    return url
    
@app.route('/upload', methods=['POST'])
def upload_file():
    file = request.files.get('file')
    labels = request.files.get('labels')
    if not file or file.filename == '':
        return jsonify({'error': 'No TIFF file selected'}), 400
    if not allowed_file(file.filename):
        return jsonify({'error': 'Invalid TIFF file type'}), 400

    filename = file.filename
    labels_data = labels.read() if labels and labels.filename else None

    existing = GeoTIFF.query.filter_by(filename=filename).first()
    if existing:
        return jsonify({
            'message': 'File already exists',
            'filename': existing.filename,
            'thumbnail_url': existing.thumbnail_url,
            'tiff_url': existing.tiff_url,
        }), 200

    try:
        file_data = file.read()
        # Generate thumbnail
        with rasterio.open(BytesIO(file_data)) as src:
            if src.count == 1:
                thumb = src.read(1, out_shape=(src.height // 4, src.width // 4), resampling=rasterio.enums.Resampling.bilinear)
                thumb = np.stack([thumb]*3, axis=-1)
            else:
                thumb = src.read(indexes=[1, 2, 3], out_shape=(3, 256, 256), resampling=rasterio.enums.Resampling.bilinear)
                thumb = reshape_as_image(thumb)
            min_val, max_val = thumb.min(), thumb.max()
            if max_val - min_val < 1e-9:
                thumb = np.zeros_like(thumb, dtype=np.uint8)
            else:
                thumb = ((thumb - min_val) / (max_val - min_val + 1e-9) * 255).astype(np.uint8)
            with rasterio.MemoryFile() as memfile:
                with memfile.open(driver='JPEG', height=thumb.shape[0], width=thumb.shape[1], count=3, dtype='uint8') as dst:
                    dst.write(thumb.transpose(2, 0, 1))
                thumb_bytes = memfile.read()

        # Upload TIFF and thumbnail to Supabase Storage
        tiff_url = upload_to_supabase_storage(file_data, filename)
        thumb_name = filename.rsplit('.', 1)[0] + '_thumbnail.jpg'
        thumbnail_url = upload_to_supabase_storage(thumb_bytes, thumb_name)
        labels_url = None
        if labels_data:
            labels_name = filename.rsplit('.', 1)[0] + '_labels.tif'
            labels_url = upload_to_supabase_storage(labels_data, labels_name)

        new_tiff = GeoTIFF(
            filename=filename,
            tiff_url=tiff_url,
            thumbnail_url=thumbnail_url,
            labels_url=labels_url
        )
        db.session.add(new_tiff)
        db.session.commit()
        return jsonify({
            'filename': filename,
            'thumbnail_url': thumbnail_url,
            'tiff_url': tiff_url,
            'message': 'File uploaded successfully'
        })
    except rasterio.RasterioIOError as e:
        return jsonify({'error': f'Invalid TIFF: {str(e)}'}), 400
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': str(e)}), 500

@app.route('/api/images')
def get_images():
    images = GeoTIFF.query.all()
    result = []
    for img in images:
        result.append({
            'id': img.id,
            'filename': img.filename,
            'tiff_url': img.tiff_url,
            'thumbnail_url': img.thumbnail_url,
        })
    return jsonify(result)

@app.route('/api/tiff/<int:id>')
def get_tiff(id):
    img = GeoTIFF.query.get_or_404(id)
    return jsonify({'tiff_url': img.tiff_url})

@app.route('/api/thumbnail/<int:id>')
def get_thumbnail(id):
    img = GeoTIFF.query.get_or_404(id)
    return jsonify({'thumbnail_url': img.thumbnail_url})

@app.route('/')
def serve_index():
    return send_file('index.html')

if __name__ == '__main__':
    with app.app_context():
        db.create_all()
    app.run(debug=True)