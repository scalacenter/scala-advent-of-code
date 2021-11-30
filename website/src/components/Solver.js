import React from 'react'
import solver from '../js/solver'

const Solver = ({puzzle}) => {
  React.useEffect(() => solver(puzzle), [])
  return (
    <div id={puzzle}></div>
  )
}

export default Solver
