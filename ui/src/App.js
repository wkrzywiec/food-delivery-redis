import './App.css';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Table from 'react-bootstrap/Table';

import { useRef, useState } from 'react';

import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min";

function App() {

  const [searchData, setSearchData] = useState([])
  const [basketData, setBasketData] = useState([])

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
            setSearchData(data)
        })
        .catch(error => {
            console.error('There was an error!', error);
        });
  }

  function addToBasket(e) {
    e.preventDefault();
    console.log('Adding to basket item with id: ' + e.target.id)
    // setBasketData([])

    const meal = searchData.find( ({id}) => id === e.target.id)
    console.log(meal)

    const mealInBasket = basketData.find( ({id}) => id === e.target.id)
    
    if (mealInBasket) {
      // mealInBasket.amount = mealInBasket.amount + 1
      const result = []
      basketData.forEach(item => {
        if (item.id === meal.id) {
          item.amount = item.amount + 1
        }
        result.push(item)
      })

      setBasketData(result)

    } else {
      meal.amount = 1
      setBasketData([...basketData, meal])
    }

    
  }



  return (
    <div className="App">
      <Container>
        <Row className="header">
          <Col><h1>Food Delivery</h1></Col>
        </Row>

        <Row>
          <Col><h3>Find meals</h3></Col>
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
        <Row className="foodSearchResults">
          Results:
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>Name</th>
                <th>Price</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {searchData.map((item, i) => (
                    <tr key={i}>
                        <td>{item.name}</td>
                        <td>{item.pricePerItem}</td>
                        <td><Button variant="primary" type="submit" id={item.id} onClick={addToBasket}>Add</Button></td>
                    </tr>
                ))}
            </tbody>
          </Table>
        </Row>

        <Row>
          <Col><h3>Current order</h3></Col>
        </Row>
        <Row className="foodSearchResults">
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>Name</th>
                <th>Price</th>
                <th>Amount</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {basketData.map((item, i) => (
                    <tr key={i}>
                        <td>{item.name}</td>
                        <td>{item.pricePerItem}</td>
                        <td>{item.amount}</td>
                        <td><Button variant="danger" type="submit">Remove</Button></td>
                    </tr>
                ))}
            </tbody>
          </Table>
        </Row>

      </Container>
    </div>
  );
}

export default App;
