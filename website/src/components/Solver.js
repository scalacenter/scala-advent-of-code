import React from 'react';
import solver from '../js/solver';

function Solver(props) {
  React.useEffect(() => {solver(props.puzzle)}, [])
  return (<div id={props.puzzle}></div>);
}

export default Solver;
