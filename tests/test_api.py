import pytest
from app import app  # Import your Flask app

@pytest.fixture
def client():
    app.config['TESTING'] = True  # Enable testing mode
    with app.test_client() as client:
        yield client

def test_hello_route(client):
    response = client.get('/hello')
    assert response.status_code == 200
    assert response.data.decode() == 'Hello, World!'
