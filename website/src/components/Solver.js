import React from 'react'
import solver from '../js/solver'

const Solver = (props) => {
  const { puzzle } = props
  React.useEffect(() => {
    if ('year' in props) {
      const { year } = props
      solver(puzzle, year)
    } else {
      solver(puzzle)
    }
  }, [])
  return (
    <div id={puzzle}></div>
  )
}

export default Solver
