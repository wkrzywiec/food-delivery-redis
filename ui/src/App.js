import './App.css';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';

import { useRef } from 'react';

import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min";

function App() {

  const foodSearch = useRef(null);
  
  function handleFoodSearch(e) {
    e.preventDefault();
    console.log('Searching for meals with phrase: ' + foodSearch.current.value)

    fetch('http://localhost:8081/foods?q=' + foodSearch.current.value)
        .then(async response => {
            const data = await response.json();

            // check for error response
            if (!response.ok) {
                // get error message from body or default to response statusText
                const error = (data && data.message) || response.statusText;
                return Promise.reject(error);
            }
            
            console.log(data)
        })
        .catch(error => {
            console.error('There was an error!', error);
        });
  }

  return (
    <div className="App">
      <Container>
        <Row className="header">
          <Col><h1>Food Delivery</h1></Col>
        </Row>
        <Form>
        <Row>
          <Col className="searchFood">
            <Form.Group className="mb-3" controlId="formBasicFood">
              <Form.Control placeholder="Enter food name" className="searchFoodContent" ref={foodSearch} />
            </Form.Group>
          </Col>

          <Col className="searchFood">
            <Button variant="success" type="submit" className="searchFoodContent" onClick={handleFoodSearch}>
              Find
            </Button>
          </Col>
        </Row>
        </Form>
      </Container>
    </div>
  );
}

export default App;
